pipeline {
  agent any

  tools { maven 'MavenLocal' }

  options { timestamps() }

  stages {
    stage('Checkout') {
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

    stage('Publish Extent Report (HTML)') {
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

    stage('Create ZIP of AutomationReports') {
      steps {
        script {
          echo "Workspace: ${env.WORKSPACE}"
          if (isUnix()) {
            sh '''
              set -x
              rm -f AutomationReports.zip || true
              if [ -d AutomationReports ]; then
                zip -r AutomationReports.zip AutomationReports || true
              else
                echo "WARNING: AutomationReports directory not found"
              fi
              ls -l AutomationReports* || true
            '''
          } else {
            // On Windows: create zip with PowerShell and show file info
            bat '''
              if exist AutomationReports.zip del /F /Q AutomationReports.zip
              if exist AutomationReports (
                powershell -NoProfile -Command "Compress-Archive -Path 'AutomationReports\\*' -DestinationPath 'AutomationReports.zip' -Force"
              ) else (
                echo WARNING: AutomationReports directory not found
              )
              powershell -NoProfile -Command "Get-ChildItem -Force AutomationReports* | Select-Object Name,Length | Format-Table -AutoSize"
            '''
          }
        }
      }
    }

    stage('Verify attachments exist') {
      steps {
        script {
          // print some diagnostics so we can see if files are present at post time
          if (isUnix()) {
            sh '''
              echo "=== ls -la (workspace) ==="
              ls -la || true
              echo "=== ls -la AutomationReports ==="
              ls -la AutomationReports || true
              if [ -f AutomationReports.zip ]; then
                echo "AutomationReports.zip exists - size:"
                stat -c "%n %s bytes" AutomationReports.zip || true
              else
                echo "AutomationReports.zip MISSING"
              fi
              if [ -f AutomationReports/TestAutomationReport.html ]; then
                echo "TestAutomationReport.html exists"
              else
                echo "TestAutomationReport.html MISSING"
              fi
            '''
          } else {
            bat '''
              echo === dir (workspace) ===
              dir /a
              echo === dir AutomationReports ===
              if exist AutomationReports ( dir AutomationReports ) else ( echo AutomationReports missing )
              if exist AutomationReports.zip (
                powershell -NoProfile -Command "Get-Item AutomationReports.zip | Select-Object Name,Length | Format-Table -AutoSize"
              ) else (
                echo AutomationReports.zip MISSING
              )
              if exist "AutomationReports\\TestAutomationReport.html" ( echo TestAutomationReport.html exists ) else ( echo TestAutomationReport.html MISSING )
            '''
          }
        }
      }
    }

    stage('Archive Artifacts (optional)') {
      steps {
        archiveArtifacts artifacts: 'AutomationReports.zip, AutomationReports/**, target/**, test-output/**', fingerprint: true, allowEmptyArchive: true
      }
    }
  }

  post {
    always {
      script {
        // debug: show the exact workspace paths we will attach
        echo "DEBUG: workspace is '${env.WORKSPACE}'"
        echo "DEBUG: attempting to attach 'AutomationReports.zip' and 'AutomationReports/TestAutomationReport.html'"

        def subject = "${env.JOB_NAME} #${env.BUILD_NUMBER} - ${currentBuild.currentResult}"
        def body = """
          <p>Build: <a href='${env.BUILD_URL}'>${env.BUILD_URL}</a></p>
          <p>Result: ${currentBuild.currentResult}</p>
          <p>See attached AutomationReports.zip and TestAutomationReport.html (if present)</p>
        """

        // IMPORTANT: credentialsId must match the Jenkins credential you created.
        // - Use Username with password credential whose username is your full gmail address and password is the app password.
        // - If you configured Extended E-mail Notification globally (Manage Jenkins) with credentials, emailext may use that instead.
        // We'll pass both possible param names used by different versions of email-ext plugin:
        emailext(
          to: 'suryarajan.selvarajan@gmail.com',
          subject: subject,
          body: body,
          mimeType: 'text/html',
          // cover both commonly used keys:
          attachPatterns: 'AutomationReports.zip, AutomationReports/TestAutomationReport.html',
          attachmentsPattern: 'AutomationReports.zip, AutomationReports/TestAutomationReport.html',
          from: 'suryarajan.selvarajan@gmail.com',
          replyTo: 'suryarajan.selvarajan@gmail.com',
          credentialsId: 'gmail-creds'
        )

        // Extra debug log - plugin often prints attachments in console; check console for "Attaching file"
        echo "Email step executed (check console for attachment lines)."
      }
    }

    success { echo "✅ Build passed" }
    unstable { echo "⚠️ Build unstable (tests failed)" }
    failure { echo "❌ Build failed" }
  }
}
