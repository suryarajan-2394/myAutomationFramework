pipeline {
    agent any

    tools {
        maven 'MavenLocal'   // Jenkins -> Global Tool Config -> Maven name
    }

    options {
        timestamps()
    }

    environment {
        EMAIL_RECIPIENTS = 'suryarajan.selvarajan@gmail.com' // recipients (comma-separated if multiple)
        ZIP_NAME = 'AutomationReports.zip'
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
                    if (isUnix()) {
                        sh 'mvn clean test -Dsurefire.suiteXmlFiles=testng.xml'
                    } else {
                        bat 'mvn clean test -Dsurefire.suiteXmlFiles=testng.xml'
                    }
                }
            }
        }

        stage('Publish Extent Report') {
            steps {
                publishHTML([
                    reportDir: 'AutomationReports',
                    reportFiles: 'TestAutomationReport.html',
                    reportName: 'Extent Report',
                    keepAll: true,
                    alwaysLinkToLastBuild: true,
                    allowMissing: true
                ])
            }
        }

        stage('Publish Reports') {
            steps {
                testNG reportFilenamePattern: '**/test-output/testng-results.xml',
                      escapeTestDescp: true, escapeExceptionMsg: true

                junit allowEmptyResults: true, testResults: '**/surefire-reports/*.xml'

                archiveArtifacts artifacts: 'AutomationReports/**, test-output/**, target/**', fingerprint: true
            }
        }

        /* ---------------- ZIP and EMAIL stages ---------------- */

        stage('Zip AutomationReports') {
            steps {
                script {
                    // Remove existing zip if present
                    if (fileExists(env.ZIP_NAME)) {
                        echo "Removing existing ${env.ZIP_NAME}"
                        deleteFile(env.ZIP_NAME)
                    }

                    if (isUnix()) {
                        sh "zip -r ${env.ZIP_NAME} AutomationReports"
                    } else {
                        // Windows PowerShell Compress-Archive
                        powershell """
                        if (Test-Path -Path '${env.ZIP_NAME}') { Remove-Item -Path '${env.ZIP_NAME}' -Force }
                        Compress-Archive -Path 'AutomationReports\\*' -DestinationPath '${env.ZIP_NAME}'
                        """
                    }

                    // sanity check
                    if (!fileExists(env.ZIP_NAME)) {
                        error "ZIP file ${env.ZIP_NAME} was not created!"
                    }
                }
            }
        }

        stage('Email Report (with ZIP)') {
            steps {
                script {
                    def subj = "[Jenkins] Automation Report - ${env.JOB_NAME} #${env.BUILD_NUMBER}"
                    def body = """
                        <p>Hi,</p>
                        <p>PFA the automation report ZIP for build <b>${env.BUILD_NUMBER}</b> of job <b>${env.JOB_NAME}</b>.</p>
                        <p>Build URL: ${env.BUILD_URL}</p>
                        <p>Regards,<br/>Jenkins</p>
                    """

                    // send email using Email Extension plugin (uses global SMTP config)
                    emailext(
                        subject: subj,
                        body: body,
                        mimeType: 'text/html',
                        to: "${env.EMAIL_RECIPIENTS}",
                        attachmentsPattern: "${env.ZIP_NAME}",
                        attachLog: true
                    )
                }
            }
        }
    }

    post {
        always {
            // archive the zip for download even if email fails
            archiveArtifacts artifacts: "${env.ZIP_NAME}", allowEmptyArchive: true
            echo "Build finished. Artifacts: ${env.BUILD_URL}artifact/"
        }
    }
}
