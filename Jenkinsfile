pipeline {

    agent{
        label "survey-agent"
    }

    tools {
        jdk 'Java17'
        maven 'MavenTool'
    }

    environment {
        VERSION_VALUE = "1.0.0-${BUILD_NUMBER}"
	    JENKINS_API_TOKEN = credentials("JENKINS_API_TOKEN")
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
                        docker_image.push("${VERSION_VALUE}")
                        docker_image.push('latest')
                    }
                }
            }
        }

        stage("Version Tag Trigger") {
            steps {
                script {
                    sh "curl -v -k --user anton:${JENKINS_API_TOKEN} -X POST -H 'cache-control: no-cache' -H 'content-type: application/x-www-form-urlencoded' --data 'VERSION_VALUE=${VERSION_VALUE}' 'http://jenkins.local:8080/job/version-refresh-pipeline/buildWithParameters?token=auth-token'"
                }
            }
        }
    }
}