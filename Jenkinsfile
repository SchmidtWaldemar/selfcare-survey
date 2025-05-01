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
