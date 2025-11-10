pipeline {
    agent any

    tools {
        // Make sure 'MavenLocal' exists in Jenkins -> Global Tool Configuration
        maven 'MavenLocal'
    }

    options {
        timestamps()
        // ansiColor removed because it caused the "Invalid option type" error
    }

    environment {
        // recipients - change if needed
        EMAIL_RECIPIENTS = 'suryarajan.selvarajan@gmail.com'
        REPORT_DIR = 'AutomationReports'
        REPORT_ZIP = 'AutomationReports.zip'
    }

    stages {
        stage('Checkout Code') {
            steps {
                checkout scm
            }
        }

        stage('Build & Run Tests') {
            steps {
                script {
                    // keep pipeline from aborting at this stage so post always executes
                    // (we mark unstable on failure below)
                    catchError(buildResult: 'UNSTABLE', stageResult: 'UNSTABLE') {
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
            steps {
                publishHTML([
                    reportDir: "${env.REPORT_DIR}",
                    reportFiles: 'TestAutomationReport.html',
                    reportName: 'Extent Report',
                    keepAll: true,
                    alwaysLinkToLastBuild: true,
                    allowMissing: true
                ])
            }
        }

        stage('Publish Test Results & Archive') {
            steps {
                script {
                    // TestNG / JUnit
                    junit allowEmptyResults: true, testResults: '**/surefire-reports/*.xml'
                    // archive full report folder & other useful outputs
                    archiveArtifacts artifacts: "${env.REPORT_DIR}/**, test-output/**, target/**", fingerprint: true, allowEmptyArchive: true
                }
            }
        }

        stage('Zip AutomationReports') {
            steps {
                script {
                    if (isUnix()) {
                        // linux / mac
                        sh """
                            rm -f ${env.REPORT_ZIP} || true
                            zip -r ${env.REPORT_ZIP} ${env.REPORT_DIR}
                        """
                    } else {
                        // windows: use PowerShell Compress-Archive
                        bat """
                            if exist ${env.REPORT_ZIP} del /F /Q ${env.REPORT_ZIP}
                            powershell -NoProfile -Command "Compress-Archive -Path '${env.REPORT_DIR}\\*' -DestinationPath '${env.REPORT_ZIP}' -Force"
                        """
                    }
                }
            }
        }

        stage('Publish ZIP as Artifact (optional)') {
            steps {
                archiveArtifacts artifacts: "${env.REPORT_ZIP}", fingerprint: true, allowEmptyArchive: true
            }
        }
    }

    post {
        always {
            script {
                // Display helpful info in console
                echo "Build result: ${currentBuild.currentResult}"
                echo "Sending email (if email-ext configured)..."

                // --- EMAIL: using email-ext plugin ---
                // Note: email-ext uses SMTP settings configured in Jenkins -> Manage Jenkins -> Configure System
                // Make sure your SMTP server, port, TLS, and credentials (gmail-creds) are configured there.
                //
                // This will attach the AutomationReports.zip (if present).
                //
                // If you want to use credentials inside pipeline to call a custom mailer, do that separately.
                //
                // The email-ext call below is the simplest: subject, body, recipients, and attachment pattern.

                // Wait a little to ensure zip was written on remote agent (avoid race)
                sleep(time: 2, unit: "SECONDS")

                // Send email with attachment (email-ext must be installed & SMTP configured)
                emailext (
                    to: "${env.EMAIL_RECIPIENTS}",
                    subject: "[${currentBuild.currentResult}] ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                    body: """Build: ${env.BUILD_URL}
Status: ${currentBuild.currentResult}
Job: ${env.JOB_NAME} #${env.BUILD_NUMBER}

See attached AutomationReports.zip (if generated).
""",
                    attachmentsPattern: "${env.REPORT_ZIP}",
                    mimeType: 'text/plain'
                )
            }
        }

        success {
            echo "✅ Build SUCCESS"
        }

        unstable {
            echo "⚠️ Build UNSTABLE (some tests failed)"
        }

        failure {
            echo "❌ Build FAILED"
        }
    }
}
