#!/usr/bin/env bash
set -euo pipefail

if [ "${EUID}" -ne 0 ]; then
  exec sudo -E bash "$0" "$@"
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RELEASE_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

APP_USER="${APP_USER:-pixel-lab}"
APP_HOME="${APP_HOME:-/opt/pixel-lab}"
TOMCAT_HOME="${TOMCAT_HOME:-/opt/tomcat9}"
TOMCAT_VERSION="${TOMCAT_VERSION:-9.0.118}"
TOMCAT_ARCHIVE="apache-tomcat-${TOMCAT_VERSION}.tar.gz"
TOMCAT_URL="https://dlcdn.apache.org/tomcat/tomcat-9/v${TOMCAT_VERSION}/bin/${TOMCAT_ARCHIVE}"

echo "[1/7] Installing system packages..."
apt-get update
DEBIAN_FRONTEND=noninteractive apt-get install -y \
  ca-certificates curl unzip nginx mysql-server openjdk-17-jdk

echo "[2/7] Creating application user and directories..."
if ! id -u "${APP_USER}" >/dev/null 2>&1; then
  useradd --system --home "${APP_HOME}" --shell /usr/sbin/nologin "${APP_USER}"
fi

install -d -o "${APP_USER}" -g "${APP_USER}" "${APP_HOME}"
install -d -o "${APP_USER}" -g "${APP_USER}" "${APP_HOME}/uploads"
install -d -o root -g root -m 0755 /etc/pixel-lab

echo "[3/7] Installing Tomcat ${TOMCAT_VERSION}..."
if [ ! -x "${TOMCAT_HOME}/bin/catalina.sh" ]; then
  tmp_dir="$(mktemp -d)"
  curl -fL "${TOMCAT_URL}" -o "${tmp_dir}/${TOMCAT_ARCHIVE}"
  tar -xzf "${tmp_dir}/${TOMCAT_ARCHIVE}" -C "${tmp_dir}"
  rm -rf "${TOMCAT_HOME}"
  mv "${tmp_dir}/apache-tomcat-${TOMCAT_VERSION}" "${TOMCAT_HOME}"
  rm -rf "${TOMCAT_HOME}/webapps/docs" \
         "${TOMCAT_HOME}/webapps/examples" \
         "${TOMCAT_HOME}/webapps/host-manager" \
         "${TOMCAT_HOME}/webapps/manager"
  chmod +x "${TOMCAT_HOME}/bin/"*.sh
  rm -rf "${tmp_dir}"
fi
chown -R "${APP_USER}:${APP_USER}" "${TOMCAT_HOME}"

echo "[4/7] Installing environment template..."
if [ ! -f /etc/pixel-lab/pixel-lab.env ]; then
  install -m 0600 -o root -g root "${RELEASE_ROOT}/env/pixel-lab.env.example" /etc/pixel-lab/pixel-lab.env
  echo "Created /etc/pixel-lab/pixel-lab.env. Edit DB_PASSWORD and DEEPSEEK_API_KEY before deployment."
fi

echo "[5/7] Installing systemd service..."
install -m 0644 -o root -g root "${RELEASE_ROOT}/server/pixel-lab-tomcat.service" /etc/systemd/system/pixel-lab-tomcat.service
systemctl daemon-reload
systemctl enable mysql
systemctl enable pixel-lab-tomcat

echo "[6/7] Installing Nginx site..."
install -m 0644 -o root -g root "${RELEASE_ROOT}/nginx/pixel-lab.conf" /etc/nginx/sites-available/pixel-lab.conf
ln -sf /etc/nginx/sites-available/pixel-lab.conf /etc/nginx/sites-enabled/pixel-lab.conf
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl enable nginx
systemctl restart nginx

echo "[7/7] Setup complete."
echo "Next:"
echo "  1. Edit /etc/pixel-lab/pixel-lab.env"
echo "  2. Run: sudo bash ${RELEASE_ROOT}/server/deploy-war.sh"
