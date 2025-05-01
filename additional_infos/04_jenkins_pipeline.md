# Jenkins Pipeline

Gestern haben wir Gitlab und Jenkins soweit vorbereitet, dass wir heute die Spring Applikation laden und ein Build daraus erstellen können. Beim Jenkins-Agent werden wir dafür Java und Maven benötigen. Da wir bei der letzten Beschreibung vergessen haben Java beim Agenten zu installieren, holen wir das jetzt nach.

## Java beim Jenkins-Agent

```
echo "deb [signed-by=/etc/apt/keyrings/adoptium.asc] https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | sudo tee /etc/apt/sources.list.d/adoptium.list

sudo apt update

# Java JDK installieren
sudo apt install temurin-17-jdk
```

## Pipeline Einstellungen vorbereiten

Bei einem Test haben wir zuletzt einen Pipeline-Script unter Jenkins angelegt und den Build gestartet. Wir können stattdessen auch eine s.g. SCM (Source Control Management) Scriptdatei unter dem Namen Jenkinsfile im Hauptordner unseres Projekts anlegen. Genau das tun wir auch innerhalb unseres Spring Application Projekts. Oben geben wir an, welchen Jenkins-Agenten wir nutzen wollen und danach welche Tools wir nutzen. Dazu zählen dann Java17 und MavenTool.
Unter Jenkins müssen wir diese Tools dann erst installiert haben. Falls noch nicht geschehen, tun wir das unter Jenkins verwalten > Plugins > Available plugins. Hier setzen wir einen Hacken unter 'Maven Integration' und 'Pipeline Maven Integration'. Dann suchen wir nach dem Java Plugin 'Temurin' und wählen hier 'Eclipse Temurin installer Plugin'. Dann klicken wir auf 'Install' und setzen den Hacken für einen Neustart des Systems nach der Installation.

Dann gehen wir erneut auf 'Jenkins verwalten' und klicken auf 'Tools'. Dann suchen wir nach der Überschrift 'Maven Installationen' und klicken dort auf einen Dropdown Button. Nun geben wir den Namen an, den wir auch in der Jenkinsfile für das Maven Tool vorgesehen haben, also 'MavenTool' und wählen am besten die aktuellste Version aus und speichern ganz unten auf 'Apply'. Auf der gleichen Seite suchen wir dann nach 'JDK' und auf 'Add JDK'. Auch hier geben wir den Namen des Tools aus der Jenkinsfile an, also 'Java17' und setzen einen Hacken auf 'Install automatically' und wählen unten in der Dropbox 'Installationsverfahren hinzufügen' und wählen 'Install from adoptium.net' aus. Hier können wir die Version auswählen, die wir auch bei unserem Agenten installiert haben. Das finden wir z.B. heraus, indem wir auf der Konsole des Agenten 'java --version' eingeben (z.B. 17.0.15+6). Falls weiter oben 'Installiere von java.sun.com' dabei steht, kann dieses Feld mit einem roten X entfernt werden. Sollten Sie jedoch eine Java Version direkt von Java-Sun laden wollen, brauchen Sie einen Account bei sun.com, unter dem Sie sich bei Jenkins anmelden müssen.
Danach klicken wir erneut auf 'Apply' und auf 'Save' und haben damit unsere Tools für den Build konfiguriert. Auf dem aktuellen Stand sieht unsere Jenkinsfile wie folgt aus:

```
pipeline {

    agent{
        label "survey-agent"
    }

    tools {
        jdk 'Java17'
        maven 'MavenTool'
    }
    
    stages{
        stage("clean workspace") {
            steps {
                cleanWs()
            }
        }
    
        stage("git pull project") {
            steps {
                git branch: 'master', credentialsId: 'gitlab-token', url: 'http://gitlab.local/selfcare/selfcare-survey'
            }
        }

        stage("build survey application") {
            steps {
                sh "mvn clean package"
            }
        }
    }
}
```

Jetzt sollten wir noch sicherstellen, dass Jenkins sich unter Gitlab authentifizieren kann. Dazu gehen wir bei Gitlab auf unser selfcare-survey Projekt, dann auf 'Setting' und auf 'Access tokens'. Beim Klick auf 'Add new token' setzen wir beim Namen 'gitlab-token' und bei Beschreibung 'Access Token from Jenkins' ein. Den generierten Token speichern wir und gehen zum Jenkins. Bei Jenkins gehen wir auf 'Jenkins verwalten' und dann auf 'Credentials'. Wir klicken unter 'Stores scoped to Jenkins' auf '(globals)' und auf 'Add credentials'.

Wir bleiben bei der Benutzername und Passwort Auswahl. Beim Benutzernamen setzen wir den Usernamen, den wir bei bei Gitlab zur Anmeldung verwenden und beim Passwort setzen wir unseren Token ein. Bei der ID setzen wir unseren 'gitlab-token', den wir auch bei der Jenkinsfile unter credentialsId festgelegt haben.

Nun gehen wir erneut zu 'Jenkins verwalten' und klicken im linken Menü auf '+ Element anlegen'. Hier geben wir den Namen 'selfcare-survey' ein und dann auf 'Pipeline'.
Beim Klick auf 'Alte Build verwerfen' können wir festlegen, wie viele Builds wir aufbewahren möchten, damit die älteren automatisch gelöscht werden. Aus eigener Erfahrung löscht Jenkins die alten Builds nicht komplett, doch bei genügend Festplattenspeicherplatz sollte das kein Problem sein, hier bei 'Maximale Anzahl an Builds, die aufbewahrt werden' die Zahl 3 zu setzen.
Den restlichen Checkboxen bleiben wie sie sind und nur beim lezten Abschnitt unter 'Pipeline' wählen wir 'Pipeline script from SCM' und bei SCM 'Git' aus. Bei der Repository URL fügen wir 'http://gitlab.local/selfcare/selfcare-survey' ein bei den Credentials unsere Zugangsdaten zu Gitlab. Solltet Ihr das Projekt auf 'Public' gesetzt haben, dann wird die Angabe zu den Credentials vermutlich nicht nötig sein, da Jenkins direkt per Git auf das Projekt zugreifen kann. Sollte nach Angabe des Repository Fehlermeldungen auftauchen, so solltet Ihr u.U. die Credentials noch mal genauer überprüfen und bei Gitlab neu setzen.
Bevor die SCM gespeichert wird, solltet ihr auch den korrekten Git-Branch setzen. Bei Github ist der master Branch in der Regel auf 'main' gesetzt. Das lässt sich auf dem lokalen Arbeitsrechner mit 'git status' überprüfen. Sollte in der Ausgabe z.B. 'origin/master' stehen, dann ist 'master' unsere Bezeichnung für den Branch den verwenden wollen. Beim Deployment kann es natürlich auch ein anderer Branch sein, wie z.B. für Testzwecke ein 'test'-Branch. In unserem Fall setzen wir unter 'Branch Specifier (blank for 'any')' den String '*/master' und klicken dann auf 'Save'.

Jetzt klicken wir links auf 'Jetzt bauen' und der Build wird z.B. unter dem Hashtag '#1' gestartet. Beim Test sollte nun der Build abbrechen. Das liegt daran, dass der Test auch einen Zugriff auf die Datenbank erfordert. Dazu installieren wir die Datenbank auf dem Jenkins-Agent, wie wir es bereits auf unserem Arbeitsrechner getan haben unter [Implementierung und Start](https://github.com/SchmidtWaldemar/selfcare-survey/blob/main/additional_infos/01_implement_application.md#implementierung-und-start).

Wenn wir erneut 'Jetzt bauen' aufrufen, sollte der Build erfolgreich durchgeführt werden. Sollten noch weitere Fehler auftauchen, so empfehle ich den Script oben in die Jenkinsfile hinzuzufügen und den Stand mit den Befehlen zu versionieren und den Build erneut durchzuführen:

```
cd ~/git/selfcare-survey
git add .
git commit -m "changed Pipelinefile"
git push
```

## was uns in der nächsten Beschreibung erwartet ...

In der nächsten Beschreibung werden wir Sonarqube installieren und beim Buildprozess innerhalb der Pipeline mit einsetzen. Damit wollen wir sicherstellen, dass unsere Spring Applikation einen qualitativen Code enthällt, bevor es für das Deployment freigegeben wird.

