#!/usr/bin/env bash
# Renews the Let's Encrypt certificate of mail.agrishare.farm and hands it to every service
# that uses it. Installed on the VPS as /usr/local/bin/certbot-renew.sh, run by root's crontab:
#   17 3 * * 1 /usr/local/bin/certbot-renew.sh >> /var/log/certbot-renew.log 2>&1
#
# Users of the certificate:
#   - our Nginx (HTTPS for mail.agrishare.farm), mounted read-only from secrets/letsencrypt;
#   - Mailcow's Postfix and Dovecot (SMTP 25/587, IMAP 993). Mailcow runs with
#     SKIP_LETS_ENCRYPT=y because port 80 belongs to our Nginx, so it does not renew
#     anything itself: it reads data/assets/ssl/{cert,key}.pem, which this script updates.
set -euo pipefail

LETSENCRYPT=/opt/agrishare/repo/secrets/letsencrypt
WEBROOT=/opt/agrishare/repo/secrets/certbot-webroot
LIVE=$LETSENCRYPT/live/mail.agrishare.farm
MAILCOW=/opt/mailcow-dockerized
MAILCOW_SSL=$MAILCOW/data/assets/ssl

# Only renews when the certificate expires within 30 days
docker run --rm \
  -v "$WEBROOT":/var/www/certbot \
  -v "$LETSENCRYPT":/etc/letsencrypt \
  certbot/certbot renew --webroot -w /var/www/certbot --quiet

docker exec agrishare-nginx nginx -s reload

# Copy to Mailcow only when it changed, so the mail services restart only after a renewal
if ! cmp -s "$LIVE/fullchain.pem" "$MAILCOW_SSL/cert.pem" || ! cmp -s "$LIVE/privkey.pem" "$MAILCOW_SSL/key.pem"; then
  cp -L "$LIVE/fullchain.pem" "$MAILCOW_SSL/cert.pem"
  cp -L "$LIVE/privkey.pem" "$MAILCOW_SSL/key.pem"
  chmod 600 "$MAILCOW_SSL/key.pem"
  (cd "$MAILCOW" && docker compose restart postfix-mailcow dovecot-mailcow nginx-mailcow)
  echo "$(date -Is) Mailcow certificate updated"
fi
