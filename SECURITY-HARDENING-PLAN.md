# Plano de Hardening de Seguranca do Servidor

**Data:** 2026-03-25
**Servidor:** srv1426770 (Hostinger VPS) — IP publico `69.62.91.201`, Tailscale `100.92.101.46`
**Baseado em:** Auditoria de seguranca realizada em 2026-03-25

---

## 1. CRITICO — Fazer AGORA (risco de comprometimento iminente)

### 1.1 Ativar UFW e bloquear portas expostas na internet

**Problema:** O firewall UFW esta INATIVO. Todos os servicos Docker com bind `0.0.0.0` estao acessiveis diretamente pela internet no IP publico `69.62.91.201`. Isso inclui PostgreSQL (5434), Lithos (8765), Paperclip (3100), Obsidian (3200/3201), apps interview-me (8080/8081), travian-bot (9090), OpenClaw Grok (55469/55470). O `orq-postgres` na porta 5434 ja foi identificado como exposto — o mesmo vetor que comprometeu os outros PostgreSQL.

**Risco se nao fizer:** Qualquer pessoa na internet pode acessar esses servicos. Bancos de dados podem ser comprometidos, dados exfiltrados, servicos usados como pivot para outros ataques.

**IMPORTANTE sobre Docker e UFW:** O Docker manipula iptables diretamente e IGNORA regras do UFW por padrao. Ativar o UFW sozinho NAO protege portas Docker. E necessario configurar o Docker para respeitar o UFW.

**Comandos:**

```bash
# PASSO 1: Configurar Docker para NAO manipular iptables diretamente
# Isso e ESSENCIAL — sem isso, UFW nao protege portas Docker
cat > /etc/docker/daemon.json << 'JSONEOF'
{
  "iptables": false,
  "default-address-pools": [
    {"base": "172.17.0.0/16", "size": 24}
  ]
}
JSONEOF

# PASSO 2: Criar regras UFW BEFORE Docker (para permitir trafego entre containers)
cat >> /etc/ufw/before.rules << 'UFWEOF'

# BEGIN DOCKER UFW RULES
*filter
:DOCKER-USER - [0:0]
:ufw-user-forward - [0:0]
-A DOCKER-USER -j RETURN -s 172.16.0.0/12
-A DOCKER-USER -j RETURN -s 10.0.0.0/8
-A DOCKER-USER -j ufw-user-forward
-A DOCKER-USER -j DROP -p tcp -m tcp --tcp-flags FIN,SYN,RST,ACK SYN -d 172.16.0.0/12
-A DOCKER-USER -j DROP -p tcp -m tcp --tcp-flags FIN,SYN,RST,ACK SYN -d 10.0.0.0/8
-A DOCKER-USER -j RETURN
COMMIT
# END DOCKER UFW RULES
UFWEOF

# PASSO 3: Configurar politica padrao do UFW
ufw default deny incoming
ufw default allow outgoing

# PASSO 4: Permitir servicos essenciais
ufw allow 22/tcp comment 'SSH'
ufw allow 80/tcp comment 'HTTP (Nginx redirect para HTTPS)'
ufw allow 443/tcp comment 'HTTPS (Nginx gateway com Cloudflare origin cert)'

# Permitir Tailscale (interface tailscale0 — acesso seguro)
ufw allow in on tailscale0 comment 'Tailscale VPN — todo trafego'

# PASSO 5: Ativar UFW
ufw --force enable

# PASSO 6: Reiniciar Docker para aplicar daemon.json
systemctl restart docker

# PASSO 7: Recriar containers (necessario apos restart do Docker)
cd /home/unando/.openclaw && docker compose -f orquestration-compose.yaml --env-file .env.orquestration up -d
cd /var/opt/workspaces/interviewme/interview-me-prj && docker compose up -d
cd /var/opt/workspaces/interviewme/interview-me-prj/others/docker && docker compose up -d

# PASSO 8: Verificar
ufw status verbose
ss -tlnp | grep "0.0.0.0"
```

**ATENCAO:** Teste o acesso SSH ANTES de fechar a sessao atual. Tenha o console Hostinger VNC aberto como backup caso o UFW bloqueie o SSH.

**Abordagem alternativa (mais simples, sem desabilitar iptables do Docker):**
Se preferir nao mexer no `daemon.json`, pode-se fazer o bind dos containers em `127.0.0.1` no compose (veja item 1.2) e manter o UFW apenas para portas nao-Docker. Essa abordagem e mais segura e previsivel.

---

### 1.2 Bind de containers Docker em 127.0.0.1 (complementar ao UFW)

**Problema:** Mesmo com UFW, a protecao de portas Docker depende da chain DOCKER-USER. A solucao mais robusta e fazer os servicos internos escutarem apenas em `127.0.0.1`. Atualmente expostos em `0.0.0.0`:

| Container | Porta | Bind atual |
|-----------|-------|------------|
| orq-postgres | 5434 | 0.0.0.0 |
| lithos | 8765 | 0.0.0.0 |
| paperclip | 3100 | 0.0.0.0 |
| obsidian | 3200, 3201 | 0.0.0.0 |
| interview-me-prod-app | 8080 | 0.0.0.0 |
| interview-me-dev-app | 8081 | 0.0.0.0 |
| openclaw-grok | 55469, 55470 | 0.0.0.0 |
| travian-bot | 9090 | 0.0.0.0 |

**Risco se nao fizer:** Qualquer servico sem autenticacao (Lithos, Paperclip, Obsidian, PostgreSQL) fica acessivel a qualquer IP na internet.

**Comandos:**

**a) orquestration-compose.yaml** (`/home/unando/.openclaw/orquestration-compose.yaml`):
```yaml
# ANTES (vulneravel):
ports:
  - "5434:5432"    # orq-postgres
  - "8765:8765"    # lithos
  - "3100:3100"    # paperclip
  - "3200:3000"    # obsidian HTTP
  - "3201:3001"    # obsidian HTTPS

# DEPOIS (seguro — acessivel apenas via localhost/Tailscale):
ports:
  - "127.0.0.1:5434:5432"    # orq-postgres
  - "127.0.0.1:8765:8765"    # lithos
  - "127.0.0.1:3100:3100"    # paperclip
  - "127.0.0.1:3200:3000"    # obsidian HTTP
  - "127.0.0.1:3201:3001"    # obsidian HTTPS
```

**b) docker-compose.yml de producao** (`/var/opt/workspaces/interviewme/interview-me-prj/docker-compose.yml`):
```yaml
# ANTES:
ports:
  - "8080:8080"    # interview-me-prod-app

# DEPOIS:
ports:
  - "127.0.0.1:8080:8080"
```

**c) docker compose dev** (`/var/opt/workspaces/interviewme/interview-me-prj/others/docker/compose.yaml`):
```yaml
# ANTES:
ports:
  - "8081:8080"    # interview-me-dev-app

# DEPOIS:
ports:
  - "127.0.0.1:8081:8080"
```

**d) Grok container** — recriar com bind local:
```bash
# Parar o container atual
docker stop openclaw-grok && docker rm openclaw-grok

# Recriar com bind 127.0.0.1 (ajustar flags conforme o docker run original)
# Verificar: docker inspect openclaw-grok antes de remover para copiar todas as flags
docker run -d --name openclaw-grok \
  -p 127.0.0.1:55469:55444 \
  -p 127.0.0.1:55470:55446 \
  --restart always \
  openclaw-grok:latest
```

**e) travian-bot** — mesmo tratamento:
```bash
# Se tiver compose, alterar "9090:8087" para "127.0.0.1:9090:8087"
# Se foi docker run, recriar com -p 127.0.0.1:9090:8087
```

**Aplicar:**
```bash
cd /home/unando/.openclaw
docker compose -f orquestration-compose.yaml --env-file .env.orquestration up -d
chown -R unando:unando /home/unando/.openclaw/orquestration-compose.yaml

cd /var/opt/workspaces/interviewme/interview-me-prj
docker compose up -d

cd /var/opt/workspaces/interviewme/interview-me-prj/others/docker
docker compose up -d
```

**Acesso via Tailscale continua funcionando** porque o Tailscale faz NAT para o IP local — `http://100.92.101.46:3100` ainda alcanca `127.0.0.1:3100`.

---

### 1.3 Trocar senha do orq-postgres e remover defaults

**Problema:** O `orq-postgres` usa senha do `.env.orquestration` (`47bfc120ea...`), mas a porta estava exposta em `0.0.0.0`. Se essa senha foi capturada, o banco esta comprometido. Alem disso, os containers interview-me usam fallback `postgres`/`postgres` quando variaveis nao estao definidas.

**Risco se nao fizer:** Atacantes que ja capturaram a senha continuam com acesso mesmo apos fechar a porta. Defaults `postgres:postgres` sao os primeiros que bots tentam.

**Comandos:**

```bash
# 1. Gerar nova senha para orq-postgres
NEW_ORQ_PW=$(openssl rand -hex 24)
echo "Nova senha orq-postgres: $NEW_ORQ_PW"

# 2. Atualizar .env.orquestration
sed -i "s/^ORQ_POSTGRES_PASSWORD=.*/ORQ_POSTGRES_PASSWORD=$NEW_ORQ_PW/" \
  /home/unando/.openclaw/.env.orquestration
chown unando:unando /home/unando/.openclaw/.env.orquestration

# 3. Alterar a senha no banco rodando (antes de recriar o container)
docker exec -it orq-postgres psql -U paperclip -d paperclip -c \
  "ALTER USER paperclip WITH PASSWORD '$NEW_ORQ_PW';"

# 4. Recriar containers
cd /home/unando/.openclaw
docker compose -f orquestration-compose.yaml --env-file .env.orquestration up -d

# 5. Verificar que o .env do interview-me prod tem senha forte (ja tem: 66FlhgMqHWyCql5kMKk3LAaT)
# Mas verificar que o dev tambem nao usa default:
cat /var/opt/workspaces/interviewme/interview-me-prj/others/docker/.env 2>/dev/null
# Se nao existir .env no dev, o fallback "postgres:postgres" e usado — criar um:
cat > /var/opt/workspaces/interviewme/interview-me-prj/others/docker/.env << 'ENVEOF'
IM_PG_PASSWORD=dev-$(openssl rand -hex 12)
IM_JWT_SECRET=$(openssl rand -base64 32)
ENVEOF
```

---

### 1.4 Credenciais de e-mail em /etc/environment (world-readable)

**Problema:** O arquivo `/etc/environment` (permissao `644` = legivel por TODOS os usuarios) contem senhas de app Google em texto claro:
- `MAIL_PASSWORD="qbviqfhizaqerbbs"` (app password do Gmail)
- Qualquer usuario do sistema (incluindo `jarvis-opc`, `ubuntu`, containers) pode ler.

**Risco se nao fizer:** Qualquer processo comprometido no servidor tem acesso imediato a conta de e-mail. App passwords do Google permitem enviar e-mails como o usuario.

**Comandos:**

```bash
# 1. Remover credenciais do /etc/environment (manter apenas PATH)
cat > /etc/environment << 'EOF'
PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/games:/usr/local/games:/snap/bin"
EOF

# 2. As credenciais de e-mail ja estao nos .env dos Docker composes
#    (interview-me/.env) — os containers nao precisam do /etc/environment

# 3. Verificar que os containers ainda funcionam
docker restart interview-me-prod-app interview-me-dev-app

# 4. Se algum servico systemd precisava dessas vars, usar EnvironmentFile:
# Em /etc/systemd/system/servico.service adicionar:
# [Service]
# EnvironmentFile=/etc/interview-me/mail.env  (com permissao 600)

# Criar arquivo seguro se necessario:
mkdir -p /etc/interview-me
cat > /etc/interview-me/mail.env << 'MAILEOF'
MAIL_USERNAME=tech.fernando.gomes@gmail.com
MAIL_PASSWORD=qbviqfhizaqerbbs
MAILEOF
chmod 600 /etc/interview-me/mail.env
```

---

## 2. ALTO — Fazer esta semana (melhorias significativas)

### 2.1 Hardening do SSH — desabilitar login root por senha

**Problema:** SSH tem `PermitRootLogin yes` e `PasswordAuthentication` no padrao (yes). O servidor recebe ~600 tentativas de brute force por dia (591 nas ultimas 24h, 643 total no fail2ban, 191 bans). IPs da subnet `2.57.122.x` (botnet conhecida) voltam repetidamente porque o ban e de apenas 1 hora.

**Risco se nao fizer:** Com ban de 1h, um atacante persistente tenta 3 senhas a cada hora = 72 tentativas/dia. Se a senha de root for fraca, sera descoberta.

**Comandos:**

```bash
# 1. ANTES DE TUDO: verificar que chaves SSH estao configuradas
cat /root/.ssh/authorized_keys
cat /home/unando/.ssh/authorized_keys
# Ambos ja tem chaves (confirmado). Se nao tivessem, PARAR e configurar primeiro.

# 2. Editar /etc/ssh/sshd_config
cp /etc/ssh/sshd_config /etc/ssh/sshd_config.bak.$(date +%Y%m%d)

cat >> /etc/ssh/sshd_config << 'SSHEOF'

# === HARDENING 2026-03-25 ===
PermitRootLogin prohibit-password
PasswordAuthentication no
PubkeyAuthentication yes
MaxAuthTries 3
LoginGraceTime 20
ClientAliveInterval 300
ClientAliveCountMax 2
# Desabilitar metodos fracos
ChallengeResponseAuthentication no
KbdInteractiveAuthentication no
UsePAM yes
SSHEOF

# 3. Testar a config ANTES de aplicar
sshd -t
# Se retornar erro, corrigir antes de continuar

# 4. Aplicar
systemctl reload sshd

# 5. TESTE: Abrir OUTRA sessao SSH (nao fechar a atual!)
#    ssh -i ~/.ssh/chave root@69.62.91.201
#    Se funcionar, fechar a sessao de teste. Se nao, reverter:
#    cp /etc/ssh/sshd_config.bak.20260325 /etc/ssh/sshd_config && systemctl reload sshd
```

### 2.2 Hardening do fail2ban — bans progressivos e permanentes

**Problema:** Ban de 1 hora nao e suficiente. IPs de botnets (ex: `2.57.122.x`, `45.148.10.x`, `45.227.254.x`) sao banidos e voltam. 191 bans para 643 falhas = mesmos IPs sendo banidos varias vezes.

**Risco se nao fizer:** Atacantes continuam gastando recursos do servidor e, com ban curto, tem janelas para tentar mais senhas.

**Comandos:**

```bash
# 1. Backup
cp /etc/fail2ban/jail.local /etc/fail2ban/jail.local.bak.$(date +%Y%m%d)

# 2. Substituir configuracao com bans progressivos
cat > /etc/fail2ban/jail.local << 'F2BEOF'
[DEFAULT]
# Ban padrao: 24 horas
bantime = 86400
# Janela de deteccao: 1 hora
findtime = 3600
# Max tentativas
maxretry = 3
# Ignorar Tailscale e localhost
ignoreip = 127.0.0.1/8 ::1 100.64.0.0/10
# Acao de ban
banaction = iptables-multiport

[sshd]
enabled = true
port = ssh
logpath = %(sshd_log)s
backend = systemd
maxretry = 3
bantime = 86400
findtime = 3600

# Reincidentes: ban de 7 dias
[recidive]
enabled = true
logpath = /var/log/fail2ban.log
banaction = iptables-allports
bantime = 604800
findtime = 86400
maxretry = 3
F2BEOF

# 3. Reiniciar fail2ban
systemctl restart fail2ban

# 4. Verificar
fail2ban-client status
fail2ban-client status sshd
fail2ban-client status recidive

# 5. Banir permanentemente as subnets mais agressivas (opcional)
ufw deny from 2.57.122.0/24 comment 'Botnet conhecida'
ufw deny from 45.148.10.0/24 comment 'Botnet conhecida'
```

### 2.3 Remover/restringir usuarios com NOPASSWD:ALL

**Problema:** O usuario `jarvis-opc` tem `NOPASSWD:ALL` no sudoers — pode executar QUALQUER comando como root sem senha. O usuario `ubuntu` (padrao da Hostinger) tambem tem `NOPASSWD:ALL`. Se qualquer processo rodando como esses usuarios for comprometido, o atacante tem root imediato.

**Risco se nao fizer:** Escalonamento de privilegios trivial a partir de qualquer RCE em servico rodando como `jarvis-opc` ou `ubuntu`.

**Comandos:**

```bash
# 1. Verificar se jarvis-opc e usado ativamente
ps aux | grep -E "^jarvis" | grep -v grep
# Se nao tiver processos, considerar desabilitar a conta

# 2. Restringir jarvis-opc a comandos especificos (se ainda necessario)
cat > /etc/sudoers.d/jarvis-opc << 'SUDOEOF'
# jarvis-opc: apenas systemctl e journalctl (sem NOPASSWD:ALL)
jarvis-opc ALL=(ALL) NOPASSWD: /usr/bin/systemctl, /usr/bin/journalctl, /usr/bin/docker
SUDOEOF
chmod 440 /etc/sudoers.d/jarvis-opc

# 3. Restringir ubuntu tambem (cloud-init default)
cat > /etc/sudoers.d/90-cloud-init-users << 'SUDOEOF'
# ubuntu: restrito a comandos de manutencao
ubuntu ALL=(ALL) NOPASSWD: /usr/bin/systemctl, /usr/bin/journalctl
SUDOEOF
chmod 440 /etc/sudoers.d/90-cloud-init-users

# 4. Validar sintaxe sudoers (NUNCA pular este passo!)
visudo -c
# Se retornar "parsed OK", esta certo. Se der erro, corrigir IMEDIATAMENTE.

# 5. Se jarvis-opc nao e mais necessario, desabilitar:
# usermod -L jarvis-opc  # Bloqueia login
# usermod -s /usr/sbin/nologin jarvis-opc  # Impede shell
```

### 2.4 OpenClaw gateway: trocar --bind lan para --bind localhost

**Problema:** O OpenClaw escuta em `0.0.0.0:55444` e `0.0.0.0:55445` (`--bind lan` = todas interfaces). Esses gateways ficam expostos na internet publica.

**Risco se nao fizer:** Qualquer pessoa pode tentar acessar o gateway de IA diretamente, potencialmente interagindo com agentes ou explorando vulnerabilidades do OpenClaw.

**Comandos:**

```bash
# O acesso externo ja e via Tailscale Serve (HTTPS:443 -> 127.0.0.1:55444)
# e via Nginx gateway (443 -> 55444). Ambos acessam localhost.
# Entao podemos fazer bind apenas em 127.0.0.1 + Tailscale IP.

# 1. Editar servico principal
cp /etc/systemd/system/openclaw.service /etc/systemd/system/openclaw.service.bak

# Trocar: --bind lan -> --bind 127.0.0.1
sed -i 's/--bind lan --port 55444/--bind 127.0.0.1 --port 55444/' \
  /etc/systemd/system/openclaw.service

# 2. Editar servico Marcele
cp /etc/systemd/system/openclaw-marcele.service /etc/systemd/system/openclaw-marcele.service.bak

sed -i 's/--bind lan --port 55445/--bind 127.0.0.1 --port 55445/' \
  /etc/systemd/system/openclaw-marcele.service

# 3. Aplicar
systemctl daemon-reload
systemctl restart openclaw openclaw-marcele

# 4. Verificar
ss -tlnp | grep -E "55444|55445"
# Deve mostrar 127.0.0.1:55444 e 127.0.0.1:55445 (nao 0.0.0.0)

# 5. Testar acesso via Tailscale
curl -s http://127.0.0.1:55444/ready | python3 -m json.tool
curl -s http://127.0.0.1:55445/ready | python3 -m json.tool
```

**NOTA:** Se o Docker (Paperclip, Grok) precisa acessar o gateway via IP do host Docker bridge (`172.18.0.1`), sera necessario bind em `0.0.0.0` OU configurar o OpenClaw para escutar tambem na interface Docker bridge. Nesse caso, o UFW/iptables deve bloquear acesso externo a essas portas.

---

## 3. MEDIO — Fazer este mes (boas praticas)

### 3.1 JWT Secret padrao no interview-me

**Problema:** O fallback do JWT_SECRET e a string literal `your-256-bit-secret-must-be-at-least-32-characters-long-for-HS256`. Se o `.env` nao definir `JWT_SECRET`, qualquer pessoa que conhege esse default pode forjar tokens JWT e acessar a API como qualquer usuario.

**Risco se nao fizer:** Se por qualquer razao o `.env` nao for carregado (container recriado, compose atualizado), a aplicacao roda com secret conhecido publicamente (esta no codigo-fonte).

**Comandos:**

```bash
# 1. Gerar JWT secret forte
JWT_NEW=$(openssl rand -base64 48)

# 2. Adicionar ao .env de producao
echo "JWT_SECRET=$JWT_NEW" >> /var/opt/workspaces/interviewme/interview-me-prj/.env

# 3. Para dev, gerar outro
JWT_DEV=$(openssl rand -base64 48)
echo "IM_JWT_SECRET=$JWT_DEV" >> /var/opt/workspaces/interviewme/interview-me-prj/others/docker/.env

# 4. Recriar containers
cd /var/opt/workspaces/interviewme/interview-me-prj && docker compose up -d
cd /var/opt/workspaces/interviewme/interview-me-prj/others/docker && docker compose up -d

# 5. IMPORTANTE: tokens existentes serao invalidados (usuarios precisam re-logar)
```

### 3.2 Proteger arquivos .env com permissoes restritas

**Problema:** Arquivos `.env` com senhas podem estar legiveis por outros usuarios.

**Risco se nao fizer:** Qualquer usuario do sistema pode ler credenciais de banco, API keys, etc.

**Comandos:**

```bash
# Restringir permissoes de todos os .env
chmod 600 /var/opt/workspaces/interviewme/interview-me-prj/.env
chmod 600 /home/unando/.openclaw/.env.orquestration
chmod 600 /home/unando/.openclaw/.env 2>/dev/null

# Verificar ownership
chown unando:unando /home/unando/.openclaw/.env.orquestration
chown unando:unando /home/unando/.openclaw/.env 2>/dev/null

# Verificar
ls -la /var/opt/workspaces/interviewme/interview-me-prj/.env
ls -la /home/unando/.openclaw/.env*
```

### 3.3 Limpar processos zombie

**Problema:** 12 processos zombie (`cat`, `curl`) acumulados desde 2026-03-14 e 2026-03-22. Zombies indicam processos pais que nao colheram o exit status dos filhos — possivelmente crons com `exec` que falharam silenciosamente (exatamente o cenario documentado como "NEVER" nas regras do OpenClaw).

**Risco se nao fizer:** Zombies consomem PIDs (limite do kernel). Em volume alto, podem esgotar a tabela de processos.

**Comandos:**

```bash
# 1. Identificar processos pais dos zombies
ps aux | awk '$8 ~ /Z/' | awk '{print $2}' | while read zpid; do
  echo "Zombie PID $zpid -> Parent $(cat /proc/$zpid/status 2>/dev/null | grep PPid | awk '{print $2}')"
done

# 2. Se o pai for PID 1 (systemd), so reboot limpa
# Se o pai for outro processo, matar o pai libera os zombies:
# kill -SIGCHLD <PPID>  # Pede ao pai para colher zombies
# Se nao funcionar:
# kill <PPID>  # Mata o pai (zombies viram filhos do init e sao limpos)

# 3. Para prevenir no futuro, auditar crons do OpenClaw:
# Verificar se existem crons com exec que podem gerar zombies
grep -r "exec" /home/unando/.openclaw/cron/ 2>/dev/null | grep -v runs/
```

### 3.4 Adicionar rate limiting no Nginx gateway

**Problema:** O Nginx gateway (`opc-nginx-gateway`) aceita conexoes na porta 443 sem rate limiting. Um atacante pode fazer brute force na UI web ou sobrecarregar o servico.

**Risco se nao fizer:** DoS via flood de requests ao gateway OpenClaw.

**Comandos:**

```bash
# Localizar config do Nginx no container
docker exec opc-nginx-gateway cat /etc/nginx/nginx.conf

# Adicionar na secao http (antes dos server blocks):
#   limit_req_zone $binary_remote_addr zone=gateway:10m rate=10r/s;
#
# E nos location blocks:
#   limit_req zone=gateway burst=20 nodelay;
#   limit_req_status 429;

# Recarregar Nginx
docker exec opc-nginx-gateway nginx -s reload
```

### 3.5 Habilitar Docker content trust e scan de imagens

**Problema:** Containers sao construidos e executados sem verificacao de integridade ou scan de vulnerabilidades.

**Risco se nao fizer:** Imagens base podem conter vulnerabilidades conhecidas.

**Comandos:**

```bash
# 1. Ativar content trust (verifica assinaturas de imagens)
echo 'export DOCKER_CONTENT_TRUST=1' >> /root/.bashrc

# 2. Scan de vulnerabilidades nas imagens atuais
docker scout quickview 2>/dev/null || docker scan --accept-license postgres:17-alpine
# Se docker scout nao estiver disponivel:
# apt install -y trivy
# trivy image postgres:17-alpine
# trivy image pgvector/pgvector:pg18
# trivy image ollama/ollama:latest
```

---

## 4. BAIXO — Quando possivel (melhorias adicionais)

### 4.1 Implementar log centralizado para tentativas de acesso

**Problema:** Logs estao espalhados entre journalctl, Docker logs, fail2ban log, auth.log. Dificil correlacionar eventos de seguranca.

**Risco se nao fizer:** Deteccao lenta de incidentes. Dificuldade de forense pos-incidente.

**Comandos:**

```bash
# Criar script de resumo de seguranca diario
cat > /usr/local/bin/security-digest.sh << 'SCRIPT'
#!/bin/bash
echo "=== Security Digest $(date) ==="
echo ""
echo "--- SSH Brute Force (ultimas 24h) ---"
journalctl -u ssh --since "24h ago" | grep -c "Failed password"
echo "tentativas de login falhadas"
echo ""
echo "--- fail2ban Status ---"
fail2ban-client status sshd
echo ""
echo "--- Portas abertas em 0.0.0.0 (deveria ser vazio apos hardening) ---"
ss -tlnp | grep "0.0.0.0" | grep -v "127.0.0.1"
echo ""
echo "--- Processos Zombie ---"
ps aux | awk '$8 ~ /Z/' | wc -l
echo ""
echo "--- Containers com restart recente (possivel crash loop) ---"
docker ps --format "{{.Names}} {{.Status}}" | grep -i "restarting"
echo ""
echo "--- Conexoes externas ativas ---"
ss -tnp | grep -v "127.0.0.1" | grep -v "100.92.101" | grep ESTAB | head -20
SCRIPT

chmod +x /usr/local/bin/security-digest.sh

# Executar diariamente via cron
echo "0 8 * * * root /usr/local/bin/security-digest.sh >> /var/log/security-digest.log 2>&1" \
  > /etc/cron.d/security-digest
```

### 4.2 Configurar Tailscale SSH (eliminar SSH publico)

**Problema:** SSH na porta 22 e o principal vetor de ataque (591 tentativas em 24h). Se todo acesso SSH for via Tailscale, a porta 22 pode ser fechada para a internet.

**Risco se nao fizer:** Continua expondo SSH para brute force (mitigado por fail2ban + chave-only, mas a superficie de ataque permanece).

**Comandos:**

```bash
# 1. Habilitar Tailscale SSH
tailscale up --ssh

# 2. Testar acesso via Tailscale SSH
# De outro dispositivo no tailnet:
# ssh root@100.92.101.46

# 3. Se funcionar, fechar SSH publico (manter Tailscale)
ufw delete allow 22/tcp
ufw allow in on tailscale0 to any port 22 comment 'SSH apenas via Tailscale'

# ATENCAO: Manter console Hostinger VNC como fallback!
# Se Tailscale cair, o unico acesso sera pelo painel Hostinger.
```

### 4.3 Rotacionar app password do Gmail

**Problema:** A app password `qbviqfhizaqerbbs` estava exposta em `/etc/environment` (world-readable) e em `.env` files. Pode ter sido capturada.

**Risco se nao fizer:** Se a senha foi capturada, o atacante pode enviar e-mails como `tech.fernando.gomes@gmail.com`.

**Passos:**

1. Ir em https://myaccount.google.com/apppasswords
2. Revogar a app password atual
3. Gerar nova app password
4. Atualizar em:
   - `/var/opt/workspaces/interviewme/interview-me-prj/.env`
   - `/etc/interview-me/mail.env` (se criado no passo 1.4)
5. Reiniciar containers: `docker restart interview-me-prod-app interview-me-dev-app`

### 4.4 Auditar e remover containers/servicos desnecessarios

**Problema:** `travian-bot` (9090), `openclaw-grok` (55469/55470), `openclaw-grokproxy` — podem ser servicos de teste ou abandonados que aumentam a superficie de ataque.

**Risco se nao fizer:** Cada servico exposto e um vetor potencial. Servicos esquecidos nao recebem patches.

**Comandos:**

```bash
# Listar containers e seus uptime/restart count
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# Para cada container desnecessario:
# docker stop <nome> && docker rm <nome>

# Verificar se ha imagens orfas consumindo disco
docker image prune -a --dry-run
```

---

## Resumo: Ordem de Execucao Recomendada

| # | Item | Prioridade | Tempo estimado |
|---|------|-----------|----------------|
| 1 | 1.2 Bind Docker containers em 127.0.0.1 | CRITICO | 15 min |
| 2 | 1.4 Remover credenciais do /etc/environment | CRITICO | 5 min |
| 3 | 1.3 Trocar senha orq-postgres | CRITICO | 10 min |
| 4 | 1.1 Ativar UFW | CRITICO | 20 min |
| 5 | 2.1 Hardening SSH | ALTO | 10 min |
| 6 | 2.2 Hardening fail2ban | ALTO | 10 min |
| 7 | 2.4 OpenClaw --bind localhost | ALTO | 10 min |
| 8 | 2.3 Restringir NOPASSWD:ALL | ALTO | 10 min |
| 9 | 3.1 JWT Secret | MEDIO | 10 min |
| 10 | 3.2 Permissoes .env | MEDIO | 5 min |
| 11 | 3.3 Limpar zombies | MEDIO | 10 min |
| 12 | 3.4 Rate limiting Nginx | MEDIO | 15 min |
| 13 | 4.1-4.4 Nice-to-haves | BAIXO | 1-2 horas |

**Tempo total estimado para CRITICO + ALTO: ~90 minutos**

---

## Checklist Pos-Hardening

```bash
# Executar apos todas as mudancas:

# 1. UFW ativo e bloqueando
ufw status verbose

# 2. Nenhuma porta Docker em 0.0.0.0 (exceto 80/443 do Nginx se necessario)
ss -tlnp | grep "0.0.0.0" | grep -v -E "(0.0.0.0:80|69.62.91.201:443)"
# Resultado esperado: vazio

# 3. SSH seguro
sshd -T | grep -E "permitrootlogin|passwordauthentication|pubkeyauthentication"
# Esperado: prohibit-password, no, yes

# 4. fail2ban com recidive
fail2ban-client status

# 5. OpenClaw funcionando
curl -s http://127.0.0.1:55444/ready | python3 -m json.tool
curl -s http://127.0.0.1:55445/ready | python3 -m json.tool

# 6. Servicos Docker funcionando
docker ps --format "{{.Names}}: {{.Status}}" | sort

# 7. Sem credenciais em /etc/environment
cat /etc/environment  # Deve ter apenas PATH

# 8. Acesso Tailscale OK
# De outro dispositivo: curl -s http://100.92.101.46:55444/ready

# 9. Acesso publico via Nginx OK
# curl -s https://opc.fhgomes.com/  (deve funcionar via Cloudflare)
```
