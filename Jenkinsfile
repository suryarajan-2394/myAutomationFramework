pipeline {
    agent any

    tools {
        maven 'Mavenlocal'
    }

    options {
        timestamps()
    }

    stages {
        stage('Checkout Code') {
            steps {
                checkout([$class: 'GitSCM',
                    branches: [[name: '*/main']],
                    userRemoteConfigs: [[url: 'https://github.com/suryarajan-2394/myAutomationFramework.git']]
                ])
            }
        }

        stage('Build & Run Tests') {
            steps {
                script {
                    catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                        bat "mvn clean test -Dsurefire.suiteXmlFiles=testng.xml"
                    }
                }
            }
        }

        stage('Publish Extent Report (HTML)') {
            steps {
                publishHTML(target: [
                    allowMissing: true,
                    alwaysLinkToLastBuild: true,
                    keepAll: true,
                    reportDir: 'AutomationReports',
                    reportFiles: 'index.html',
                    reportName: 'Extent Report'
                ])
            }
        }

        stage('Publish Test Results & Archive') {
            steps {
                script {
                    junit 'target/surefire-reports/*.xml'
                    archiveArtifacts artifacts: 'AutomationReports/**', allowEmptyArchive: true
                }
            }
        }

        stage('Zip AutomationReports') {
            steps {
                script {
                    bat """
                        if exist AutomationReports.zip del /F /Q AutomationReports.zip
                        powershell -NoProfile -Command "Compress-Archive -Path 'AutomationReports/*' -DestinationPath 'AutomationReports.zip' -Force"
                    """
                }
            }
        }

        stage('Optional: Publish ZIP as Artifact') {
            steps {
                archiveArtifacts artifacts: 'AutomationReports.zip', allowEmptyArchive: true
            }
        }
    }

    post {
        always {
            script {
                echo "Sending email with AutomationReports.zip"
                emailext(
                    to: 'suryarajan.selvarajan@gmail.com',
                    subject: "Jenkins Build #${BUILD_NUMBER} - ${currentBuild.currentResult}",
                    body: """
                        <h3>Jenkins Build Summary</h3>
                        <p>Build URL: <a href="${BUILD_URL}">${BUILD_URL}</a></p>
                        <p>Extent Report: <a href="${BUILD_URL}Extent_20Report">Click here to view Extent Report</a></p>
                        <p>Result: <b>${currentBuild.currentResult}</b></p>
                    """,
                    mimeType: 'text/html',
                    attachmentsPattern: 'AutomationReports.zip'
                )
            }
        }

        unstable {
            echo "⚠️ Pipeline unstable (some tests failed) - build result: UNSTABLE"
        }

        failure {
            echo "❌ Pipeline failed!"
        }

        success {
            echo "✅ Pipeline successful!"
        }
    }
}
