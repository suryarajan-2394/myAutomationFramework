pipeline {
    agent any

    tools {
        maven 'MavenLocal'    // make sure this exact name exists in Manage Jenkins -> Global Tool Configuration
    }

    options {
        timestamps()
        ansiColor('xterm')   // optional, helps readability
    }

    environment {
        REPORT_DIR = "AutomationReports"
        ZIP_NAME = "AutomationReports.zip"
    }

    stages {
        stage('Checkout Code') {
            steps {
                checkout scm
            }
        }

        stage('Build & Run Tests') {
            steps {
                // do not fail the whole pipeline on test failures — mark UNSTABLE instead
                catchError(buildResult: 'UNSTABLE', stageResult: 'UNSTABLE') {
                    script {
                        if (isUnix()) {
                            sh 'mvn clean test -Dsurefire.suiteXmlFiles=testng.xml'
                        } else {
                            bat 'mvn clean test -Dsurefire.suiteXmlFiles=testng.xml'
                        }
                    }
                }
            }
        }

        stage('Publish Extent Report (HTML)') {
            when {
                expression { fileExists("${env.REPORT_DIR}/TestAutomationReport.html") }
            }
            steps {
                publishHTML ([
                    reportDir: "${env.REPORT_DIR}",
                    reportFiles: 'TestAutomationReport.html',
                    reportName: 'Extent Report',
                    keepAll: true,
                    alwaysLinkToLastBuild: true,
                    allowMissing: false
                ])
            }
        }

        stage('Publish Test Results & Archive') {
            steps {
                // publish test results (junit/testng)
                junit allowEmptyResults: true, testResults: '**/surefire-reports/*.xml'
                // also archive full report folder and test-output
                archiveArtifacts artifacts: "${env.REPORT_DIR}/**, test-output/**, target/**", fingerprint: true
            }
        }

        stage('Zip AutomationReports') {
            steps {
                script {
                    if (isUnix()) {
                        sh """
                        if [ -f ${ZIP_NAME} ]; then rm -f ${ZIP_NAME}; fi
                        cd ${REPORT_DIR}
                        zip -r ../${ZIP_NAME} . || true
                        """
                    } else {
                        // Windows PowerShell
                        bat """
                        if exist ${ZIP_NAME} (del /F /Q ${ZIP_NAME})
                        powershell -NoProfile -NonInteractive -Command "Compress-Archive -Path '${REPORT_DIR}\\*' -DestinationPath '${ZIP_NAME}' -Force"
                        """
                    }
                }
            }
        }

        stage('Optional: Publish ZIP as Artifact') {
            steps {
                archiveArtifacts artifacts: "${ZIP_NAME}", fingerprint: true
            }
        }

        stage('Email Report (with ZIP)') {
            steps {
                script {
                    // send email with credentials stored in Jenkins (username/password)
                    // Ensure you have created a credential of type "Username with password" with ID = gmail-creds
                    withCredentials([usernamePassword(credentialsId: 'gmail-creds', usernameVariable: 'GMAIL_USER', passwordVariable: 'GMAIL_PASS')]) {
                        // emailext plugin usage
                        emailext (
                            to: 'suryarajan.selvarajan@gmail.com',
                            subject: "[$env.JOB_NAME #${env.BUILD_NUMBER}] ${currentBuild.currentResult} - Extent Report",
                            body: """<p>Build: <a href="${env.BUILD_URL}">${env.JOB_NAME} #${env.BUILD_NUMBER}</a></p>
                                     <p>Result: ${currentBuild.currentResult}</p>
                                     <p>Attached: ${ZIP_NAME}</p>""",
                            mimeType: 'text/html',
                            from: "${GMAIL_USER}",
                            replyTo: "${GMAIL_USER}",
                            attachmentsPattern: "${ZIP_NAME}"
                        )
                    }
                }
            }
        }
    }

    post {
        success {
            echo "✅ Build Succeeded: ${env.BUILD_URL}"
        }
        unstable {
            echo "⚠️ Build Unstable (some tests failed). Email still sent with report."
        }
        failure {
            echo "❌ Build Failed (error). If email didn't send, check credential / SMTP settings."
        }
        always {
            echo "Pipeline finished. Artifacts: ${env.BUILD_URL}artifact/"
        }
    }
}
