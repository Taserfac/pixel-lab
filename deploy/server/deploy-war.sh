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
ENV_FILE="${ENV_FILE:-/etc/pixel-lab/pixel-lab.env}"
WAR_FILE="${RELEASE_ROOT}/release/ROOT.war"
SQL_FILE="${RELEASE_ROOT}/sql/init.sql"

if [ ! -f "${ENV_FILE}" ]; then
  echo "Missing ${ENV_FILE}. Run setup-ubuntu22.sh first." >&2
  exit 1
fi
if [ ! -f "${WAR_FILE}" ]; then
  echo "Missing ${WAR_FILE}. Upload a packaged release first." >&2
  exit 1
fi
if [ ! -f "${SQL_FILE}" ]; then
  echo "Missing ${SQL_FILE}." >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
. "${ENV_FILE}"
set +a

DB_NAME="${DB_NAME:-pixel_lab}"
DB_USER="${DB_USER:-pixel_lab_app}"
DB_PASSWORD="${DB_PASSWORD:-}"

if [ "${DB_NAME}" != "pixel_lab" ]; then
  echo "This deployment script expects DB_NAME=pixel_lab because init.sql uses that schema." >&2
  exit 1
fi
if [ -z "${DB_PASSWORD}" ] || [ "${DB_PASSWORD}" = "CHANGE_ME_STRONG_PASSWORD" ]; then
  echo "Set a real DB_PASSWORD in ${ENV_FILE} before deployment." >&2
  exit 1
fi
if [ "${DEEPSEEK_API_KEY:-}" = "CHANGE_ME_DEEPSEEK_KEY" ]; then
  echo "Set a real DEEPSEEK_API_KEY in ${ENV_FILE} before deployment." >&2
  exit 1
fi
if [[ ! "${DB_USER}" =~ ^[A-Za-z0-9_]+$ ]]; then
  echo "DB_USER may contain only letters, numbers, and underscores." >&2
  exit 1
fi
if [[ "${DB_PASSWORD}" == *"'"* || "${DB_PASSWORD}" == *"\\"* ]]; then
  echo "DB_PASSWORD must not contain single quotes or backslashes for this setup script." >&2
  exit 1
fi

run_mysql_root() {
  if [ -n "${MYSQL_ROOT_PASSWORD:-}" ]; then
    MYSQL_PWD="${MYSQL_ROOT_PASSWORD}" mysql -uroot "$@"
  else
    mysql "$@"
  fi
}

echo "[1/5] Preparing MySQL database and app user..."
run_mysql_root <<SQL
CREATE DATABASE IF NOT EXISTS \`pixel_lab\`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
CREATE USER IF NOT EXISTS '${DB_USER}'@'127.0.0.1' IDENTIFIED BY '${DB_PASSWORD}';
ALTER USER '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
ALTER USER '${DB_USER}'@'127.0.0.1' IDENTIFIED BY '${DB_PASSWORD}';
GRANT ALL PRIVILEGES ON \`pixel_lab\`.* TO '${DB_USER}'@'localhost';
GRANT ALL PRIVILEGES ON \`pixel_lab\`.* TO '${DB_USER}'@'127.0.0.1';
FLUSH PRIVILEGES;
SQL
run_mysql_root < "${SQL_FILE}"

echo "[2/5] Preparing upload directory..."
install -d -o "${APP_USER}" -g "${APP_USER}" "${APP_HOME}/uploads"

echo "[3/5] Deploying ROOT.war to Tomcat..."
systemctl stop pixel-lab-tomcat >/dev/null 2>&1 || true
rm -rf "${TOMCAT_HOME}/webapps/ROOT" "${TOMCAT_HOME}/webapps/ROOT.war"
install -m 0644 -o "${APP_USER}" -g "${APP_USER}" "${WAR_FILE}" "${TOMCAT_HOME}/webapps/ROOT.war"
chown -R "${APP_USER}:${APP_USER}" "${TOMCAT_HOME}"

echo "[4/5] Starting services..."
systemctl restart mysql
systemctl restart pixel-lab-tomcat
systemctl reload nginx

echo "[5/5] Waiting for health check..."
for i in $(seq 1 40); do
  if curl -fsS http://127.0.0.1:8080/api/health >/tmp/pixel-lab-health.json; then
    cat /tmp/pixel-lab-health.json
    echo
    echo "Pixel Lab is running behind Tomcat."
    curl -fsS http://127.0.0.1/api/health >/tmp/pixel-lab-nginx-health.json
    echo "Nginx proxy health:"
    cat /tmp/pixel-lab-nginx-health.json
    echo
    exit 0
  fi
  sleep 2
done

echo "Health check failed. Inspect logs with:" >&2
echo "  journalctl -u pixel-lab-tomcat -n 120 --no-pager" >&2
echo "  tail -n 120 ${TOMCAT_HOME}/logs/catalina.out" >&2
exit 1
