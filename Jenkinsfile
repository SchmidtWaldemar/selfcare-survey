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

        stage("Sonarqube code check start") {
            steps {
                script {
                    withSonarQubeEnv(credentialsId: 'selfcare-survey-sonarqube-token') {
                        sh "mvn sonar:sonar"
                    }
                }
            }
        }
	
        stage("Qualitygate") {
            steps {
                script {
                    waitForQualityGate abortPipeline: true, credentialsId: 'selfcare-survey-sonarqube-token'
                }
            }
        }

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
    }
}
