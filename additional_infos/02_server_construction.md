# Server einrichten

Bei der Server Infrastruktur verwenden wir wie bei unserem <a href="https://github.com/SchmidtWaldemar/selfcare-management/blob/main/additional_infos/08_CI-CD_Pipelines.md#proxmox-hosts-aufsetzen">letzten Projekt</a> ein Virtualisierungsplattform <b>Proxmox</b>. Deshalb werden wir nicht erneut auf die Installation von Proxmox näher eingehen und beginnen direkt mit dem Aufsetzen von virtuallen Maschienen für jeden Service separat. Die Einrichtungen erfolgt bei mir nicht das erste mal, weshalb ich aus Erfahrung volgende Hardwarevoraussetzungen auf dem Proxmox läuft empfehlen würde:

- CPU: mit mindestens 4 virtuellen Kernen
- RAM: mindestens 16 GB
- Festplatte: mindestens 256 GB (besser 512 GB)

## Gitlab einrichten

Gitlab kann sowohl Online genutzt oder auch lokal auf dem eigenen Rechner installiert werden. Wir entscheiden uns für die lokale Variante. Bei Proxmox verwenden wir folgende Einstellungen dazu:

- CPU: 2 virtuelle Kerne
- RAM: 6 GB
- Festplatte: 32 GB

Bei mir wird der Ubunu Server mit der Version 22.04 verwendet. Vor etwa halben Jahr gab es mit Kubernetes einige Kompatibilitätsprobleme mit neueren Ubuntu Version 24.04, weshalb die ältere Version noch immer von mir eingesetzt wird. Hier gehe ich davon aus, dass inzwischen solche Kompatibilitätsprobleme nicht mehr existieren, weshalb die Version 24.04 oder neuer unter Umständen auch funktionieren sollte.

Während Installation von Ubuntu kann ich nur empfehlen, keine weiteren angebotenen Pakete mit zu installieren. Diese Pakete werden in der Regel vom Paketdienst Snap genutzt und konnten zumindest bei mir nur schwer auf die eigenen Wünsche umkonfiguriert werden. Hier empfehle ich auf den Paketdienst <b>apt</b> zu setzen.

Nach der Installation aktuallisieren wir das Betriebssystem wie folgt:

```
sudo apt update
sudo apt upgrade
```

Nun können wir die Installationsanleitung direkt unter Gitlab nutzen:

https://about.gitlab.com/install/#ubuntu

Oder nach dem heutigen Stand die folgenden Befehle in der Konsole ausführen (wobei wir hier auf unnötige Tools wie postfix verzichten):

```
sudo apt-get install -y curl openssh-server ca-certificates tzdata perl

# gitlab wird fuer die Installation vorbereitet
curl https://packages.gitlab.com/install/repositories/gitlab/gitlab-ee/script.deb.sh | sudo bash

# gitlab wird installiert
sudo apt-get install gitlab-ee
```

Die Konfiguration nehmen wir etwas unkonventionell vor, indem wir die Konfigurationsdatei direkt ändern:

```
sudo vi /etc/gitlab/gitlab.rb
```

Innerhalb der Datei ändern wir die Parameter wie folgt:

```
# wir nutzen den ungesicherten http Protokoll, da wir ohnehin nur lokal arbeiten werden
external_url 'http://gitlab.local'

registry_external_url 'http://gitlab.local'

gitlab_rails['registry_enabled'] = true

# den registry_host, port und path können wir unter Umständen auch ausgeklammert lassen
gitlab_rails['registry_host'] = "registry.gitlab.example.com"
gitlab_rails['registry_port'] = "5005"
gitlab_rails['registry_path'] = "/var/opt/gitlab/gitlab-rails/shared/registry"

# wird benötigt, um später die Container zu registrieren
registry['enable'] = true

registry['registry_http_addr'] = "gitlab.local:5000"
registry['log_directory'] = "/var/log/gitlab/registry"
```

Wir wollen GitLab von überall unter der Adresse http://gitlab.local zugreifen, deshalb ändern wir auf diesem Rechner die hosts Datei wie folgt:

```
sudo vi /etc/hosts
127.0.0.1   gitlab.local
```

Auf den anderen Rechnern wie dem Arbeitsrechner oder innerhalb der Clouds von Jenkins und Co verwenden wir die IP Adresse, die wir vom DHCP erhalten haben:

```
sudo apt install net-tools

ifconfig

ens18: flags=4163<UP,BROADCAST,RUNNING,MULTICAST>  mtu 1500
        inet 192.168.178.186  netmask 255.255.255.0  broadcast 192.168.178.255

sudo vi /etc/hosts
192.168.178.186   gitlab.local
```

Nun lassen wir die vorgenommenen Einstellungen wirken, indem wir Gitlab neu starten:

```
sudo gitlab-ctl restart

# oder

reboot
```

Außerdem möchten wir direkt auf den Server zugreifen. Oben haben wir bereits <b>openssh-server</b> mit apt-get installiert. Deshalb können wir nun auf diesen Rechner per SSH einwählen bzw. es auch ohne Passwort-Prompt tun:

```
ssh-copy-id user@192.168.178.186
```

Am besten wir erstellen auch gleich einen neuen SSH Schlüssel (wobei wir auf Passworteingabe einfach ohne Eingabe auf Enter klicken):

```
ssh-keygen -t rsa
```

Nun sollten wir Gitlab unter dem Browser aufrufen können:

http://gitlab.local

Der Benutzername ist 'root' und den Initialpasswort erhalten wir indem wir den folgenden Befehl aufrufen:

```
sudo cat /etc/gitlab/initial_root_password
```

Nach dem Login ändern wir das Passwort unter:

http://gitlab.local/-/user_settings/password/edit

Und damit haben wir erfolgreich Gitlab installiert.

## Jenkins einrichten

Auch Jenkins werden wir nur lokal nutzen und keine Verbindungen aus dem Internet heraus zulassen. Für die Installation könnt Ihr auch die folgende Seite nutzen:

https://www.jenkins.io/doc/book/installing/linux/

Oder auch die vereinfachte Anleitung von mir verwenden:

- CPU: 2 virtuelle Kerne
- RAM: 4 GB
- Festplatte: mindestens 50 GB

Weniger als 50 GB Festplattenspeicher kann schon bald dazu führen, dass wegen Speicherplatzmangel die Jobs nicht mehr ausgeführt werden können. In so einem Fall beschreibe ich unten, wie man die Jobs manuell bereinigen kann.


```
sudo apt update
sudo apt upgrade


echo "deb [signed-by=/etc/apt/keyrings/adoptium.asc] https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | sudo tee /etc/apt/sources.list.d/adoptium.list

sudo apt update

# Java JDK installieren
sudo apt install temurin-17-jdk


sudo wget -O /etc/apt/keyrings/jenkins-keyring.asc https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key
echo "deb [signed-by=/etc/apt/keyrings/jenkins-keyring.asc]" https://pkg.jenkins.io/debian-stable binary/ | sudo tee /etc/apt/sources.list.d/jenkins.list > /dev/null

sudo apt-get update

# Jenkins installieren
sudo apt-get install jenkins

# Jenkins starten
sudo systemctl start jenkins
```

Nun kann Jenkins schon aufgerufen werden. Der Service läuft unter dem Port 8080. Um die IP und das Initial-Passwort herauszufinden, könnt ihr wie folgt vorgehen:

```
sudo apt install net-tools

ifconfig

ens18: flags=4163<UP,BROADCAST,RUNNING,MULTICAST>  mtu 1500
        inet 192.168.178.117  netmask 255.255.255.0  broadcast 192.168.178.255

# auf dem Arbeitsrechner
sudo vi /etc/hosts
192.168.178.117   jenkins.local

# initial Passwort anzeigen
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
```

Und dann mit dem Browser aufrufen und Passwort ändern:

http://jenkins.local:8080


### Jenkins Jobs manuell bereinigen

Jenkins führt die Jobs intern mit Docker aus. Wenn nicht genügend Festplattenspeicher vorhanden ist, lassen sich die historischen Jobs nicht innerhalb von Jenkins entfernen. Dadurch können die Agens blockiert werden und somit auch den gesammten Job-Prozess unmöglich machen. Deshalb fand ich nur die einzige Möglichkeit zur manuellen Bereinigung alter Jobs, um so die Blockade aufzuheben. Die einzelnen Container von Docker lassen wie folgt entfernt: 

```
cd /var/lib/docker/overlay2/
du -shx * | sort -rh | head -10

# möglichst nicht den obersten Paket auswählen und entfernen mit z.B:
rm -rf opix0ffptgyfa89b1cmetf1d3
```

Danach können die Agenten nach Seiten-Aktuallisierung wieder einsatzbereit gemacht werden.


## Weitere Systeme einrichten

Die nächsten Services wie Sonarqube und ArgoCD werden wir ebenfalls bei Proxmox einrichten, sobald diese benötigt werden. Insgesamt werden wir noch 3 Virtuell Maschinen verwenden, um unsere gewünschte Architektur zu verwirklichen. Auch die Einrichtung von Gitlab und Jenkins werden wir in den nänchsten Versionen genauer beschreiben.
