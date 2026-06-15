# Pixel Lab Alibaba Cloud Deployment

This package deploys Pixel Lab as one Tomcat application:

```text
Internet -> Nginx :80/:443 -> Tomcat 9 :8080 -> ROOT.war -> MySQL 8
```

The first public launch uses the server public IP. After ICP filing is complete, point the domain to the same server and enable HTTPS.

## 1. Buy the server

- Vendor: Alibaba Cloud Simple Application Server or ECS.
- Recommended product: Simple Application Server for the first launch.
- Region: Mainland China, Hangzhou or Shanghai preferred.
- OS image: Ubuntu 22.04 LTS system image. Do not choose WordPress, BT Panel, or other app images.
- Size: 2 vCPU / 4 GB RAM / 5 Mbps or above.
- Firewall or security group: allow inbound `22`, `80`, `443`. Keep `3306` closed. Keep `8080` closed publicly unless debugging.
- Login method: password login is easiest for first deployment; SSH key login is also fine.

## 2. Build the release locally

From Windows PowerShell:

```powershell
cd F:\project\pixel-lab
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\deploy\package-release.ps1
```

The output is:

```text
deploy\build\pixel-lab-release.zip
```

## 3. Upload to the server

Replace `SERVER_IP` with the Alibaba Cloud public IP:

```powershell
scp .\deploy\build\pixel-lab-release.zip ubuntu@SERVER_IP:/tmp/
```

On the server:

```bash
sudo apt-get update
sudo apt-get install -y unzip
rm -rf /tmp/pixel-lab-release
mkdir -p /tmp/pixel-lab-release
unzip /tmp/pixel-lab-release.zip -d /tmp/pixel-lab-release
cd /tmp/pixel-lab-release
```

## 4. Install server runtime

```bash
sudo bash server/setup-ubuntu22.sh
```

Edit production secrets:

```bash
sudo nano /etc/pixel-lab/pixel-lab.env
```

Required edits:

- `DB_PASSWORD`: strong MySQL password for the app user.
- `DEEPSEEK_API_KEY`: your DeepSeek API key.

Do not commit or paste this file into chat.

## 5. Deploy the app

```bash
sudo bash server/deploy-war.sh
```

Expected health output includes:

```json
{"code":200,"msg":"success","data":{"status":"ok","deepseekConfigured":true,"database":"ok"}}
```

## 6. Open the website

Open:

```text
http://SERVER_IP
```

Also test direct frontend routes:

```text
http://SERVER_IP/dashboard
http://SERVER_IP/draw
```

Default account:

```text
admin
admin123
```

Change this password immediately after the first login.

## 7. Domain and HTTPS after ICP filing

After ICP filing is approved:

1. Add an A record from the domain to `SERVER_IP`.
2. Edit `/etc/nginx/sites-available/pixel-lab.conf` and replace `server_name _;` with the domain.
3. Reload Nginx:

```bash
sudo nginx -t
sudo systemctl reload nginx
```

4. Install a certificate:

```bash
sudo apt-get install -y certbot python3-certbot-nginx
sudo certbot --nginx -d your-domain.com
```

## 8. Useful commands

```bash
curl http://127.0.0.1:8080/api/health
curl http://127.0.0.1/api/health
sudo systemctl status pixel-lab-tomcat --no-pager
sudo journalctl -u pixel-lab-tomcat -n 120 --no-pager
sudo tail -f /var/log/nginx/pixel-lab.access.log
sudo tail -f /var/log/nginx/pixel-lab.error.log
```

To deploy a new version, rebuild and upload `pixel-lab-release.zip`, unzip it on the server, then run:

```bash
sudo bash server/deploy-war.sh
```
