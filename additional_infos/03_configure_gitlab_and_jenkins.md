# GitLab und Jenkins konfigurieren

Bei der letzten Beschreibung haben wir aufgeführt, wie man Jenkins und Gitlab unter Proxmox installiert. Nun beschreiben wir, wie man die beiden Systeme so konfiguriert, damit unsere Spring Boot Applikation darin versioniert und deployt werden kann.

## Gitlab Versionierung konfigurieren

Als erstes brauchen wir eine neue Gruppe, wo wir unseren Spring Boot Code versioniert verwalten möchten. Dazu gehen wir auf Groups > New group > Create group. Den Gruppennamen setzen wir auf 'selfcare' und 'Visible Level' auf 'public' und den Rest lassen wir unausgefüllt.

Innerhalb der Gruppe erstellen wir nun ein neues Projekt unter New project > Create blank project. Wir möchten unser Projekt aus <a href="https://github.com/SchmidtWaldemar/selfcare-survey" target=_blank>selfcare-survey</a> kopieren. Deshalb nennen wir das Projekt auch 'selfcare-survey' setzen es auf 'public' und verzichten auf eine neue 'README'.

Nun müssen wir beide Git-Projekte z.B. auf unserem Arbeitsrechner klonen. Dazu muss erstmal auf dem Rechner, wo wir arbeiten möchten den Git-Client installiert haben z.B. so:

```
sudo apt-get install git
```

Dann klonen wir die Projekte wie folgt:

```
mkdir ~/git
cd ~/git
mkdir copy
cd copy/
git clone https://github.com/SchmidtWaldemar/selfcare-survey
cd selfcare-survey/
rm -rf .git/
cd ~/git/
git clone git@gitlab.local:selfcare/selfcare-survey.git
cd selfcare-survey/
mv ~/git/copy/selfcare-survey/* ~/git/copy/selfcare-survey/.[!.]* .
```

Nun können wir unsere Resultate in die Versionsverwaltung übertragen:

```
git add .
git commit -m "project copied"
git push
```

Damit haben wir das 'selfcare-survey' Projekt erfolgreich kopiert und können es nach belieben verändern.
Bei der letzten Beschreibung haben wir in der Gitlab Konfigurationsdatei die Container-Registrierung freigeschaltet. Damit wären wir auch in der Lage Pipelines unter Gitlab zu nutzen und ein Deployment aufzubauen, wo wir auf Jenkins verzichten. Ich persönlich würde jedoch größeren Wert auf Flexibilität setzen, wo wir auch unabhängig vom Versionssystem arbeiten können. Beim Einsatz von Jenkins können wir zwischen <b>Gitlab</b> und <b>Github</b> frei wählen oder das eine System durch ein anderes leicht ersetzen. Alle Einstellungen, die wir in diesem Projekt unter Jenkins vornehmen, um auf Gitlab zuzugreifen, können wir genausogut auf Github umstellen.

## Jenkins konfigurieren

Bei unserer letzten Installationsbeschreibung zu Jenkins gingen wir bei der Mindesthardwareanforderung davon aus, dass die Builds den Festplattenspeicher irgendwann überfüllen können. Zusätzlich haben wir eine Workaround Lösung beschrieben, wie man die Docker-Container manuell entfernt, damit die Agents genügend Speicher haben, um nicht einen Deadlock zu erleiden. 
Es gibt aber ähnlich wie bei den Pipeline-Runners unter Github eine elegantere Lösung, indem die Agent-Jobs auf einem separaten System getriggert werden:

[GitHub Runner anlegen und starten](https://github.com/SchmidtWaldemar/selfcare-management/blob/main/additional_infos/08_CI-CD_Pipelines.md#github-runner-anlegen-und-starten)

Genau das wollen wir jetzt machen und installieren unter Proxmox eine weitere Virtuelle Maschine, wo wir den Jenkins Agent laufen lassen. Die Installation können wir nach den ähnlichen Anforderungen durchführen, ohne Jenkins erneut zu installieren:

[Jenkins einrichten](https://github.com/SchmidtWaldemar/selfcare-survey/blob/main/additional_infos/02_server_construction.md#jenkins-einrichten)

Nachdem wir die neue virtuelle Maschine erfolgreich installiert haben, setzen wir unter /etc/hosts den Hostnamen (sowohl auf der Jenkins-UI was laut Beschr. unter 192.168.178.117 befindet als auch auf dem neuen Server) auf 'jenkins-agent.local' und erstellen einen User 'jenkins':

```
# falls nach Installation nicht vorhanden, wird jenkins User angelegt
sudo adduser jenkins
sudo usermod -aG sudo jenkins
su - jenkins
```

Nun arbeiten wir als jenkins User weiter und installieren Docker entweder nach der offiziellen Anleitung unter https://docs.docker.com/engine/install/ubuntu/ oder auch so:

```
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt update

sudo apt-get install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

Dann fügen wir den jenkins User zu der docker Gruppe hinzu, damit später keine Fehlermeldungen bei beim Zugriff von Docker Sockets entstehen:

```
sudo groupadd docker
sudo usermod -aG docker jenkins
newgrp docker
```

Sollte dennoch eine Fehlermeldung entstehen, dann können Sie auf dem Agenten Server folgenden Befehl ausführen:

```
sudo chmod 666 /var/run/docker.sock
```

In den nächsten Schritten möchten wir den Jenkins-Agent Server unter der Jenkins Weboberfläche miteinander verbinden. Das können wir mit einer SSH Verbindung tun. Deshalb legen wir beim Jenkins-Agent einen neuen SSH Schlüssel an, mit deren privaten Schlüssel Jenkins in Verbindung tritt:

```
# format PEM das Jenkins für Verbindung erwartet
ssh-keygen -t rsa -m PEM -C "Jenkins Agent"
cat .ssh/id_rsa.pub >> .ssh/authorized_keys
chmod 700 .ssh/
chmod 600 .ssh/authorized_keys .ssh/id_rsa

# wenn wir eine direkte Verbindung zur UI möchten
ssh-copy-id   jenkins@jenkins.local

# verbindung nötig, damit unter known_hosts der Server bekannt ist
ssh jenkins@jenkins.local
```

Jetzt wechseln wir auf die Jenkins Web-Oberfläche und fügen einen Agent hinzu, indem wir auf Manage jenkins > Nodes gehen und dann auf 'New Node'. Hier setzen wir den Namen 'survey-agent' und wählen den 'statischer Agent' aus. Dann setzen wir 'Number of executers' auf 2 und das 'remote Directory' auf '/home/jenkins' (oder wie auch immer der HOME Verzeichnis lautet) und beim Label erneut 'survey-agent', was wir später bei der Pipeline verwenden werden.
Bei der Startmethode wählen wir 'Starte Agent mit 'SSH' aus und geben die IP oder Hostnamen ein z.B. 'jenkins-agent.local' wenn die Jenkins-UI die Adresse unter /etc/hosts kennt oder in meinem Fall '192.168.178.114'.
Bei den Zugangsdaten drücken wir auf '+ Add' und klicken auf Jenkins. Nun wählen wir unter 'Kind' 'SSH Benutzername und privater Schlüssel' aus. Bei ID und Beschreibung setzen wir z.B. 'jenkins-agent-key' ein. Benutzername ist wie der von SSH 'jenkins' und bei privater Schlüssel klicken wir auf 'Direkt eingeben' und dann auf 'Add'. In das Feld fügen wir nun den Privaten Schlüssel ein, den wir bei dem 'jenkins-agent.local' Server wie folgt aufrufen:

```
ssh jenkins@jenkins-agent.local

cat ~/.ssh/id_rsa
```

Da wir auf ein Passwort Promt verzichtet haben, können wir das Feld leer lassen und auf Add klicken. Nun können wir unter http://jenkins.local:8080/computer/survey%2Dagent/log überprüfen, ob die SSH Verbindung aufgebaut werden kann. Falls nicht, folgen Sie die Anweisungen aus den Logs.

Nun testen wir, ob unser Agent einen Job ausführen kann. Dazu gehen wir beim Dashboard auf 'Element anlegen', setzen einen Namen fest z.B. 'test', klicken auf Pipeline und auf OK. Ganz unten können wir den Skript z.B. Hello World auswählen und auf Save klicken. Doch wir tragen einen eigenen Script ein:

```
pipeline {
    agent{
        label "survey-agent"
    }

    stages {
        stage('Hello survey agent') {
            steps {
                echo 'Hello World by survey agent'
            }
        }
    }
}
```

Wenn der Job mit dem Hashtag '#1' erfolgreich durchläuft (grün), dann haben wir praktisch den Job unter jenkins-agent.local Server erfolgreich ausführen können.

## was uns in der nächsten Beschreibung erwartet ...

In der nächsten Beschreibung wollen wir u.a. Jenkins und Gitlab miteinander koppeln und unsere Spring Boot Applikation dazu nutzen, um mithilfe der Pipeline einen Build zu erstellen.
