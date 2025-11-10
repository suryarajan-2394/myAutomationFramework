pipeline {
  agent any

  tools {
    // optional if you configured Maven in Jenkins Tool config
    // maven 'MavenLocal'
  }

  options { timestamps() }

  stages {
    stage('Checkout Code') {
      steps { checkout scm }
    }

    stage('Build & Run Tests') {
      steps {
        script {
          // Run mvn but don't abort pipeline if tests fail.
          // catchError will mark stage as UNSTABLE/SUCCESS depending on outcome
          catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
            if (isUnix()) {
              sh 'mvn clean test -Dsurefire.suiteXmlFiles=testng.xml'
            } else {
              bat 'mvn clean test -Dsurefire.suiteXmlFiles=testng.xml'
            }
          }
        } // script
      } // steps
    }

    stage('Publish Extent Report (HTML)') {
      steps {
        // publishHTML from HTML Publisher plugin
        publishHTML([
          reportDir: 'AutomationReports',
          reportFiles: 'TestAutomationReport.html',
          reportName: 'Extent Report',
          allowMissing: true,
          alwaysLinkToLastBuild: true,
          keepAll: true
        ])
      }
    }

    stage('Publish Test Results & Archive') {
      steps {
        script {
          // TestNG reporting (use the correct plugin step name 'testNG' or junit depending plugin)
          // If plugin supports testNG step (you used earlier), ensure syntax is correct for your plugin version.
          // Here we use junit for surefire xmls and also call testNG (if plugin available).
          junit allowEmptyResults: true, testResults: '**/surefire-reports/*.xml'

          // Archive the reports & surefire outputs for download in Jenkins UI
          archiveArtifacts artifacts: 'AutomationReports/**, target/**, test-output/**', fingerprint: true
        }
      }
    }

    stage('Zip AutomationReports') {
      steps {
        script {
          if (isUnix()) {
            sh '''
              rm -f AutomationReports.zip || true
              zip -r AutomationReports.zip AutomationReports || true
            '''
          } else {
            // Windows - use PowerShell Compress-Archive
            bat '''
              if exist AutomationReports.zip del /F /Q AutomationReports.zip
              powershell -NoProfile -Command "Compress-Archive -Path 'AutomationReports\\*' -DestinationPath 'AutomationReports.zip' -Force"
            '''
          }
        }
      }
    }

    stage('Optional: Publish ZIP as Artifact') {
      steps {
        archiveArtifacts artifacts: 'AutomationReports.zip', fingerprint: true
      }
    }
  } // stages

  post {
    always {
      script {
        // Always attempt to send email with attachment (even on failure). Use credentials if required.
        // Make sure Jenkins SMTP (Manage Jenkins -> Configure System -> Extended E-mail Notification) is set
        // and the Email Extension Plugin is installed and configured.
        def recipients = 'suryarajan.selvarajan@gmail.com'
        def subject = "[Jenkins] ${currentBuild.fullDisplayName} - ${currentBuild.currentResult}"
        def body = """Build: ${env.BUILD_URL}
Result: ${currentBuild.currentResult}
See attached AutomationReports.zip for full HTML report and screenshots.
"""

        // If AutomationReports.zip exists attach it; if not, send without attachment
        if (fileExists('AutomationReports.zip')) {
          echo "Sending email with AutomationReports.zip"
          // Use the Email Extension plugin step
          emailext(
            to: recipients,
            subject: subject,
            body: body,
            mimeType: 'text/plain',
            attachmentsPattern: 'AutomationReports.zip'
          )
        } else {
          echo "AutomationReports.zip not found — sending email without attachment"
          emailext(
            to: recipients,
            subject: subject,
            body: body,
            mimeType: 'text/plain'
          )
        }
      }
    }

    success { echo "✅ Pipeline succeeded - build result: SUCCESS" }
    unstable { echo "⚠️ Pipeline unstable (some tests failed) - build result: ${currentBuild.currentResult}" }
    failure { echo "❌ Pipeline failed - build result: FAILURE" }
  }
}
