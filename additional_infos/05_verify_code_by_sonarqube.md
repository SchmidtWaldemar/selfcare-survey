# Code Verification durch Sonarqube und Docker Hub

In der letzten Beschreibung haben wir Jenkins UI, Jenkins Agent und Gitlab miteinander verbunden und damit die erste Build Version erstellen könne. Wie in unserem Architekturkonzept unter [Architekturkonzept](https://github.com/SchmidtWaldemar/selfcare-survey#architekturkonzept) zu sehen, werden wir als nächstes die Qualität unseres Codes durch Sonarqube verifizieren, bevor wir es zum Deployment freigeben. D.h. wenn der Programmiercode nicht die nötigen Qualitätskriterien erfüllt, soll ein Automatismus einschreiten und der Upload nach Docker Hub findet nicht statt. Ist der Code OK, wird eine Version als Container unter Docker Hub hochgeladen.

## Sonarqube installieren

Unter Proxmox haben wir für Sonarqube folgende Hardwareanforderungen verwendet:

- CPU: 2 virtuelle Kerne
- RAM: 4 GB
- Festplatte: mindestens 48 GB
- ubuntu-22.04.5-live-server-amd64.iso

Sobald das System bereitsteht, können wir die Installation unter dem folgenden Link installieren:

https://www.centron.de/tutorial/sonarqube-auf-ubuntu-24-04-installieren-schritt-fuer-schritt-anleitung/

Gerne können wir aber auch wie folgt vorgehen:

### Postgresql installieren

```
sudo apt install postgresql-common
sudo apt install postgresql
sudo systemctl enable postgresql
sudo systemctl start postgresql

sudo -i -u postgres
psql -U postgres

ALTER USER sonaruser WITH ENCRYPTED password 'sonar';
CREATE DATABASE sonarqube;
GRANT ALL PRIVILEGES ON DATABASE sonarqube to sonaruser;
\c sonarqube
\q
exit
```

### Java installieren

```
echo "deb [signed-by=/etc/apt/keyrings/adoptium.asc] https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | sudo tee /etc/apt/sources.list.d/adoptium.list

sudo apt update

# Java JDK installieren
sudo apt install temurin-17-jdk
```

### Sonarqube installieren

```
# System vorbereiten
sudo vi /etc/sysctl.conf

# am Ende der Datei einfuegen
vm.max_map_count=262144
fs.file-max=131072

sudo vi /etc/security/limits.d/99-sonarqube.conf

# am Ende der Datei einfuegen
sonarqube   -   nofile   65536
sonarqube   -   nproc    4096

# System neustarten
sudo reboot

# Sonarqube Hauptprogramm herunterladen
wget https://binaries.sonarsource.com/Distribution/sonarqube/sonarqube-25.4.0.105899.zip

sudo apt install unzip

unzip sonarqube-25.4.0.105899.zip
sudo mv sonarqube-25.4.0.105899 /opt/sonarqube

sudo adduser --system --no-create-home --group --disabled-login sonar
sudo chown -R sonar:sonar /opt/sonarqube

sudo vi /opt/sonarqube/conf/sonar.properties

# in Datei einfügen
sonar.jdbc.username=sonaruser
sonar.jdbc.password=sonar
sonar.jdbc.url=jdbc:postgresql://localhost:5432/sonarqube

# Service einrichten zum Start nach Neustart
sudo vi /etc/systemd/system/sonar.service
```

... in Datei eintragen:

```
[Unit]
Description=SonarQube service
After=syslog.target network.target

[Service]
Type=forking

ExecStart=/opt/sonarqube/bin/linux-x86-64/sonar.sh start
ExecStop=/opt/sonarqube/bin/linux-x86-64/sonar.sh stop

User=sonar
Group=sonar
PermissionsStartOnly=true
Restart=always

StandardOutput=syslog
LimitNOFILE=65536
LimitNPROC=4096

[Install]
WantedBy=multi-user.target
```

Sonar starten:

```
sudo systemctl start sonar
sudo systemctl enable sonar
sudo systemctl status sonar
```

### Sonarqube einrichten

Unser System hat die IP 192.168.178.64 erhalten. Die rufen wir nun im Browser wie folg auf:

http://192.168.178.64:9000

Die initiale Benutzername und Passwort lauten beide 'admin', wobei man danach das Passwort ändern sollte.

Wenn wir es in die Sonarqube Weboberfläche geschaft haben, klicken wir zunächst auf den grünen Quadrat mit einem 'A' oben rechts und dort auf 'My Account'. Hier können wir unter 'Generate Tokens' einen Token erstellen, wo bei uns der Name 'selfcare-survey-token' lautet. Der Type bleibt bei 'Global Analysis Token' und bei Expires in wählen wir 'No expiration'. Den generierten Token speichern wir, bevor der nicht mehr sichtbar ist und gehen zu Jenkins. 

Unter Jenkins gehen wir auf 'Jenkins verwalten' und auf 'Credentials'. Hier wollen wir den Token speichern indem wir auf (global) unter 'Stores scoped to Jenkins' klicken und dann auf 'Add Credentials'. Wir wählen 'Secret text' aus. Beim Secret fügen wir den Token ein und bei ID und Description tragen wir einfach 'selfcare-survey-sonarqube-token' ein.

Nun gehen wir bei Jenkins unter 'Jenkins verwalten' und dort auf 'Plugins' und auf 'Available plugins'. Wir suchen nach sonar und finden 'SonarQube Scanner for Jenkins', wo ein Hacken gesetzt wird. Dann suchen wir nach 'Quality' und finden ' Quality Gates Plugin' und 'Sonar Quality Gates Plugin', was wir ebenfalls auswählen. Dann gehen wir auf 'Install' und setzen einen Hacken für einen Systemneustart.

Nun gehen wir erneut afu 'Jenkins verwalten' und dort auf 'System' und suchen nach 'SonarQube servers'. Hier setzen wir einen Hacken auf 'Environment variables' und klicken auf 'Add SonarQube'. Beim Namen tragen wir 'sonarqube-selfcare-scanner' ein und die URL lautet bei uns 'http://192.168.178.64:9000' wie oben angegeben. Bei 'Server authentication token' wählen wir 'selfcare-survey-sonarqube-token' aus und klicken auf 'Save'.

Nun aktivieren wir den Sonarqube Scanner wo wir auf 'Jenkins verwalten' und auf 'Tools' gehen. Bei 'SonarQube Scanner Installationen' klicken wir auf 'SonarQube Scanner hinzufügen' und tragen einfach den Namen 'sonarqube-scanner' ein. Dann nach dem ausgewählten 'Automatisch installieren' wählen wir die aktuellste Version vom Sonarqube Scanner aus und klicken auf 'Save'.

Sollte uns die rote Warn-Meldung unter 'Jenkins verwalten' stören, können wir die auch ausblenden, indem wir auf 'Configure which of these warnings are shown' klicken und dort suchen wir nach 'Hidden security warnings' und 'Security warnings' können wir die Checkbox abwählen, was ausgeblendet werden soll.

Nun modifizieren wir die Jenkinsfile und tragen ein neues Stage ein, durch den der Sonarqube Build angestoßen wird:

```
stage("Sonarqube code check start") {
    steps {
        script {
            withSonarQubeEnv(credentialsId: 'selfcare-survey-sonarqube-token') {
                sh "mvn sonar:sonar"
            }
        }
    }
}
```

 Sobald die Änderung in der Jenkinsfile mit Git auf Gitlab hochgeladen sind, können wir erneut einen Build anstoßen. Ist der Build auf grün, können wir direkt bei Sonarqube uns das Prüf-Resultat ansehen, indem wir auf das Projekt 'selfcare-survey-project' klicken.

 Als nächste benötigen wir noch ein s.g. Quality gate, wodurch Sonarqube uns mitteilt, ob wir einen validen Code haben und der Pipeline Prozess fortgesetzt werden kann. Dazu tragen wir in der Jenkinsfile zunächst ein weiteres Stage ein, dass wie folgt aussieht:

```
stage("Qualitygate") {
    steps {
        script {
            waitForQualityGate abortPipeline: true, credentialsId: 'selfcare-survey-sonarqube-token'
        }
    }
}
```

Dieser Quality gate hört auf einen s.g. Webhook unter Sonarqube, der eine Rückgabe an Jenkins schickt, ob alles soweit OK ist. Diesen Webhook richten wir ein, indem wir bei Sonarqube auf Administration klicken und dort auf Configuration > Webhooks. Hier klicken wir auf 'Create' und tragen unter dem Namen 'selfcare-webhook' und unter der URL 'http://jenkins.local:8080/sonarqube-webhook/'. Das Passwort Feld lassen wir frei. Nach dem speichern laden wir die Jenkinsfile bei Gitlab nochmal hoch, allerdings können wir den Build unter Jenkins noch nicht durchführen, da der Webhook unter Sonarqube die Adresse 'jenkins.local' noch nicht kennt. Das ändern wir indem wir auf dem Sonarqube Server diesen Eintrag machen (bzw. die IP, die sich für Jenkins haben):

```
sudo vi /etc/hosts

192.168.178.117 jenkins.local
```

Nun sind wir soweit und können uns mit Docker Hub beschäftigen.

## Docker Hub einrichten

Zunächst benötigen wir einen Account unter Docker Hub. Falls noch nicht vorhanden legen wir uns diesen unter dieser Adresse an:

https://hub.docker.com/

Nach der Anmeldung gehen wir oben rechts auf den großen Buchstaben, der aus Ihrem Benuternamen stamm, was in meinem Fall 'S' ist und klicken auf 'Account Settings'. Im Menü links klicken wir dann auf 'Personal access tokens' und erstellen uns einen Token, den wir irgendwo sicher abspeichern. Nun gehen wir bei Jenkins unter 'Projekt verwalten' und klicken auf 'Credentials' und legen einen neuen 'Benutzername und Passwort' Eintrag. Der Benutzername ist der, den Sie unter Docker Hub erhalten bzw. festgelegt haben. Das Passwort ist dann der Token, den wir bei Docker Hub generiert haben und die ID und Beschreibung setzen wir einfach auf 'dockerhub'.

Als nächstes müssen wir noch einige Tools unter Jenkins installieren. Dazu gehen wir auf 'Jenkins verwalten' und dort auf 'Plugins'. Bei 'Available plugins' suchen wir nach Docker und wählen folgende einfach alle verfügbaren Tools aus, installieren diese und lassen danach das System automatisch neu starten:

* CloudBees Docker Build and Publish plugin
* Docker API Plugin
* Docker Slaves Plugin
* Docker Pipeline
* Docker plugin
* docker-build-step
* Docker Slaves Plugin


### Dockerfile anlegen

Nun legen wir unter unserem Selfcare Spring Projekt im Hauptverzeichnis die Dockerfile an z.B. mit folgendem Inhalt:

```
FROM maven:3.9.9-eclipse-temurin-17 as build
WORKDIR /app
COPY . .
RUN mvn clean install -DskipTests

FROM eclipse-temurin:17.0.15_6-jdk
WORKDIR /app
COPY --from=build /app/target/selfcare-survey.jar /app/
EXPOSE 8081
CMD ["java", "-jar","selfcare-survey.jar"]

```

Wir erhalten die aktuellste Version zu Maven bei uns mit JDK 17 unter der folgenden Adresse:

https://hub.docker.com/_/maven/

Zu der Architektur x64 und JDK 17 erhalten wir die Info zur aktuellsten Version unter:

https://adoptium.net/de/temurin/releases/?version=17&arch=x64

Mit dem Zusatz '-DskipTests' igonieren wir zunächst den Testfall unter Docker, da wir ansonsten Docker Compose nutzen müssten, um die Datenbank anzulegen, womit dann der Container den Test durchführen kann. Da wir jedoch bereits oben in der Pipeline einen Maven Build durchführen und dort auch den Test zulassen, wäre das hier nur eine doppelte Mühe. 
Die 8081 Nummer ist unsere Port Nummer, die wir unter application.yml manuell vergeben haben.

### Build Jar Dateiname anpassen

Wie im Dockerfile festgelegt, haben wir den Dateinamen 'selfcare-survey.jar' vergeben. Damit Maven uns das Jar Paket immer unter diesem eindeutigen Namen generiert und z.B. keine generische Versionsnummer hinten dranhängt, ersetzen wir in der pom.xml Datei unten die &lt;build&gt; Tags durch:

```
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <executions>
                <execution>
                    <id>repackage</id>
                    <goals>
                        <goal>repackage</goal>
                    </goals>
                    <configuration>
                        <finalName>selfcare-survey</finalName>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### Jenkinsfile anpassen zum Deployment unter Docker Hub

Bevor wir den Jenkins Build starten, fügen wir noch zum Schluss den folgenden Eintrag in die Jenkinsfile ein:

```
stage("Build and Push to Docker Hub") {
    steps {
        script {
            docker.withRegistry('', 'dockerhub') {
                docker_image = docker.build "schmidtwaldemar/selfcare-survey"
            }

            docker.withRegistry('', 'dockerhub') {
                docker_image.push("1.0.0-${BUILD_NUMBER}")
                docker_image.push('latest')
            }
        }
    }
}
```

Den Eintrag 'schmidtwaldemar' solltet Ihr durch den Benutzernamen von Eurem Docker Hub setzen. Bei 'dockerhub' String handelt es sich um den Credentials Eintrag, den wir bei Jenkins vorgenommen haben. Hier registriert sich die Pipeline mit Docker Hub, erstellt das Image und lädt es dann unter zwei Versionen durch einen Push hoch. Bei der '${BUILD_NUMBER}' nutzt Jenkins die Nummer des Build, den wir manuell durch 'Jetzt bauen' anstoßen und z.B. mit '#11' identifizieren.

Nun sollten wir in der lage sein, den Code nach Docker Hub hochzuladen, indem wir auf 'Jetzt bauen' klicken. Falls der Build erfolgreich ist und unter Docker Hub ein neues Image hochgeladen werden konnte, dann haben wir es geschaft. Wenn Sie testen möchten, ob unser QualityGate korrekt funktioniert, finden Sie einen Beispielcode unter com.platform.selfcare.adapters.input.web.MainController.java der auskommentiert ist mit einem Kommentar 'bad code example'. Entfernen Sie die Kommentierung und versuchen Sie es erneut. Dann sollte die 'abortPipeline: true' Anweisung greifen und der Upload nach Docker Hub nicht stattfinden können.

## was uns in der nächsten Beschreibung erwartet ...

In der nächsten Beschreibung nehmen wir uns ArgoCD vor. Darin rufen wir einen Kubernetes Script auf und führen so einen Launch aus, sodass wir unsere Applikation als Veröffentlicht betrachten können.
