pipeline {
    agent any

    tools {
        maven 'MavenLocal'
    }

    options {
        timestamps()
    }

    environment {
        EMAIL_RECIPIENTS = 'suryarajan.selvarajan@gmail.com'
        ZIP_NAME = 'AutomationReports.zip'
    }

    stages {
        stage('Checkout Code') {
            steps { checkout scm }
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

        // keep the Publish stages if you want them to run only on success
        stage('Publish Extent Report') {
            when { expression { currentBuild.resultIsBetterOrEqualTo('SUCCESS') } } // optional
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
            when { expression { currentBuild.resultIsBetterOrEqualTo('SUCCESS') } } // optional
            steps {
                testNG reportFilenamePattern: '**/test-output/testng-results.xml',
                      escapeTestDescp: true, escapeExceptionMsg: true

                junit allowEmptyResults: true, testResults: '**/surefire-reports/*.xml'

                archiveArtifacts artifacts: 'AutomationReports/**, test-output/**, target/**', fingerprint: true
            }
        }
    }

    post {
        // This block runs irrespective of build status
        always {
            script {
                echo "Post-build: creating ZIP and sending email (build result: ${currentBuild.currentResult})"

                // create ZIP (overwrite if exists)
                if (fileExists(env.ZIP_NAME)) {
                    echo "Deleting previous ${env.ZIP_NAME}"
                    deleteFile(env.ZIP_NAME)
                }

                if (isUnix()) {
                    // -q to reduce output, || true so it doesn't throw here
                    sh "zip -r ${env.ZIP_NAME} AutomationReports || true"
                } else {
                    // Windows PowerShell
                    powershell """
                    if (Test-Path -Path '${env.ZIP_NAME}') { Remove-Item -Path '${env.ZIP_NAME}' -Force }
                    if (Test-Path -Path 'AutomationReports') {
                        Compress-Archive -Path 'AutomationReports\\*' -DestinationPath '${env.ZIP_NAME}' -Force
                    } else {
                        Write-Output 'AutomationReports folder not found; creating empty zip'
                        New-Item -ItemType File -Path '${env.ZIP_NAME}' -Force | Out-Null
                    }
                    """
                }

                // debug: show workspace listing and zip size
                if (isUnix()) {
                    sh "ls -la || true"
                    sh "ls -la AutomationReports || true"
                    sh "ls -la ${env.ZIP_NAME} || true"
                } else {
                    powershell "Get-ChildItem -Force | Select-Object Name,Length | Format-Table"
                    powershell "if (Test-Path AutomationReports) { Get-ChildItem -Path AutomationReports -Recurse | Select-Object FullName,Length | Format-Table -AutoSize }"
                    powershell "if (Test-Path ${env.ZIP_NAME}) { (Get-Item ${env.ZIP_NAME}).Length | Write-Output 'ZIPSIZE=' }"
                }

                // Archive ZIP so it's kept in Jenkins even if email fails
                archiveArtifacts artifacts: "${env.ZIP_NAME}", allowEmptyArchive: true

                // Now send email with attachment; use global SMTP config (email-ext)
                def subject = "[Jenkins] Automation Report - ${env.JOB_NAME} #${env.BUILD_NUMBER} - ${currentBuild.currentResult}"
                def body = """<p>Hi,</p>
                              <p>Please find attached the automation report for build <b>${env.BUILD_NUMBER}</b> of job <b>${env.JOB_NAME}</b>.</p>
                              <p>Build URL: ${env.BUILD_URL}</p>
                              <p>Build result: <b>${currentBuild.currentResult}</b></p>
                              <p>Regards,<br/>Jenkins</p>"""

                // protect emailext with try/catch so we still finish post block gracefully
                try {
                    emailext(
                        subject: subject,
                        body: body,
                        mimeType: 'text/html',
                        to: "${env.EMAIL_RECIPIENTS}",
                        attachmentsPattern: "${env.ZIP_NAME}",
                        attachLog: true
                    )
                    echo "Email sent (emailext executed)."
                } catch (e) {
                    echo "Email failed: ${e}"
                    // do not change build result; just log the error
                }
            }
        }

        // Optionally set final result messages or cleanups
        cleanup {
            echo "Cleanup post actions (optional)"
        }
    }
}
