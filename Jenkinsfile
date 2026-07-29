pipeline {
    agent any

    // adding this comment to test auto delete of branches after merge
    triggers {
        pollSCM('')
        cron('H H(0-4) * * 1-5') // AutoMatically triggers a clean monitoring scan every weeknight between midnight and 4 AM
    }

    environment {
        RAW_TAG = "${env.TAG_NAME ?: ''}"
        VERSION = "${RAW_TAG ? RAW_TAG.replaceAll('^v', '') : env.GIT_COMMIT[0..7] + '-SNAPSHOT'}"
    }

    tools {
        maven 'M3'
    }

    stages {
        stage('Backend Unit Tests') {
            steps {
                sh './mvnw clean test'
            }
        }

        stage('Frontend Unit Tests') {
            agent {
                docker {
                    image 'node:20-alpine'
                    reuseNode true
                }
            }
            steps {
                dir('frontend') {
                    sh 'npm install'
                    sh 'npm run test -- --watch=false'
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('MySonarServer') {
                    sh './mvnw org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
  -Dsonar.projectKey=buy02 \
  -Dsonar.projectName=buy02 '
                }
            }
        }

        stage('Quality Gate Enforcer') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    script {
                        def qg = waitForQualityGate()
                        if (qg.status != 'OK') {
                            error "Pipeline aborted due to quality gate failure: ${qg.status}"
                        }
                    }
                }
            }
        }

        stage('Package & Deploy Artifacts') {
            steps {
                configFileProvider([configFile(fileId: '3edde727-58d2-4c8b-8c77-818369c293b5', variable: 'MAVEN_SETTINGS')]) {
                    sh 'echo $VERSION'
                    sh 'echo env.TAG_NAME'
                    sh './mvnw versions:set -DnewVersion=$VERSION -DgenerateBackupPoms=false'
                    sh './mvnw deploy -s $MAVEN_SETTINGS -DskipTests'
                }
            }
        }

        stage('Build Images & Push') {
            when {
                buildingTag()
            }
            steps {
                // Securely authenticate Docker against your local registry
                withCredentials([usernamePassword(credentialsId: 'NEXUS_CREDENTIALS',
                                                  usernameVariable: 'DOCKER_USER',
                                                  passwordVariable: 'DOCKER_PASS')]) {
                    sh 'echo "$DOCKER_PASS" | docker login localhost:8086 -u "$DOCKER_USER" --password-stdin'
                    sh 'docker compose build'
                    sh 'docker compose push'
                                                  }
            }
        }

        stage('Deploy Stack') {
            steps {
                echo 'Deploying the Microservices Platform...'
                script {
                    sh 'docker network inspect shared-net >/dev/null 2>&1 || docker network create shared-net'
                    sh 'docker compose -p buy01-current down --remove-orphans'
                    sh 'docker compose -p buy01-current up -d'
                }
            }
        }
    }
    post {
        always {
            echo 'Processing and archiving all test results...'

            junit testResults: '**/target/surefire-reports/*.xml, **/frontend/junit-frontend.xml',
                  allowEmptyResults: true
        }

        success {
            echo 'Deployment successful!'

            mail to: 'justyoupika@gmail.com',
                 subject: "Pipeline Success: Job '${env.JOB_NAME}' [Build #${env.BUILD_NUMBER}]",
                 body: "Great news! The pipeline completed successfully.\n\nView the execution details here: ${env.BUILD_URL}"
        }
        failure {
            echo 'Build failed! Executing authenticated automated rollback...'

            mail to: 'justyoupika@gmail.com',
                 subject: "🛑 PIPELINE CRASHED: Job '${env.JOB_NAME}' [Build #${env.BUILD_NUMBER}]",
                 body: "Attention! The pipeline has failed during execution.\n\nReview the console logs to debug the failure here: ${env.BUILD_URL}console"

            sh 'git revert HEAD --no-edit'

            withCredentials([usernamePassword(credentialsId: 'gitea pull credentials',
                                          usernameVariable: 'GIT_USER',
                                          passwordVariable: 'GIT_TOKEN')]) {
                sh '''

                # Configure temporary credentials for this specific push command
                git remote set-url origin "https://${GIT_USER}:${GIT_TOKEN}@learn.zone01oujda.ma/git/amazighi/mr-jenk.git"

                # Push the revert cleanly back up
                git push origin HEAD:main
            '''
                                          }
        }
    }
}
