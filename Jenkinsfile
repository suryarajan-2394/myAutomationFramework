pipeline {
  agent any

  tools {
    maven 'MavenLocal'   // leave as-is if you have 'MavenLocal' configured
  }

  options { timestamps() }

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

    stage('Create Sanitised ZIP (for email)') {
      steps {
        script {
          // Create a ZIP that contains only "safe" extensions for email attachment.
          // Adjust extensions list if you want to allow more.
          if (isUnix()) {
            sh '''
              set -e
              rm -f AutomationReports_safe.zip || true
              if [ -d AutomationReports ]; then
                rm -rf tmp_reports || true
                mkdir -p tmp_reports
                # copy safe files preserving relative paths
                find AutomationReports -type f \\( -iname "*.html" -o -iname "*.htm" -o -iname "*.css" -o -iname "*.js" -o -iname "*.png" -o -iname "*.jpg" -o -iname "*.jpeg" -o -iname "*.svg" \\) -print0 | xargs -0 -I{} rsync --relative "{}" tmp_reports/
                (cd tmp_reports && zip -r ../AutomationReports_safe.zip .) || true
                rm -rf tmp_reports
              else
                echo "AutomationReports folder not found - skipping sanitised zip creation"
              fi
              echo "Sanitised zip created:"
              ls -l AutomationReports_safe.zip || true
            '''
          } else {
            // Windows powershell flow
            bat '''
              if exist AutomationReports_safe.zip del /F /Q AutomationReports_safe.zip
              powershell -NoProfile -Command "
                \$ws = \$env:WORKSPACE
                \$src = Join-Path \$ws 'AutomationReports'
                \$tmp = Join-Path \$ws 'tmp_reports'
                if (Test-Path \$tmp) { Remove-Item -Recurse -Force \$tmp }
                if (Test-Path \$src) {
                  New-Item -ItemType Directory -Path \$tmp | Out-Null
                  Get-ChildItem -Path \$src -Recurse -File |
                    Where-Object { \$_.Extension -in '.html','.htm','.css','.js','.png','.jpg','.jpeg','.svg' } |
                    ForEach-Object {
                      \$rel = \$_.FullName.Substring(\$ws.Length+1)
                      \$dest = Join-Path \$tmp \$rel
                      New-Item -ItemType Directory -Path (Split-Path \$dest) -Force | Out-Null
                      Copy-Item -Path \$_.FullName -Destination \$dest -Force
                    }
                  Compress-Archive -Path (Join-Path \$tmp '*') -DestinationPath AutomationReports_safe.zip -Force
                  Remove-Item -Recurse -Force \$tmp
                  Write-Output 'Created AutomationReports_safe.zip'
                } else {
                  Write-Output 'AutomationReports directory not present - skipped sanitised zip'
                }
              "
            '''
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

    stage('Archive Artifacts') {
      steps {
        // archive safe zip and the whole AutomationReports folder (so HTML available in artifacts)
        archiveArtifacts artifacts: 'AutomationReports_safe.zip, AutomationReports/**', allowEmptyArchive: true, fingerprint: true
      }
    }
  }

  post {
    always {
      script {
        // build email content
        def subject = "${env.JOB_NAME} #${env.BUILD_NUMBER} - ${currentBuild.currentResult}"
        def artifactUrl = "${env.BUILD_URL}artifact/"

        def body = """
          <p>Build: <a href='${env.BUILD_URL}'>${env.BUILD_URL}</a></p>
          <p>Result: ${currentBuild.currentResult}</p>
          <p>Attached: TestAutomationReport.html (if present) and a sanitised AutomationReports_safe.zip (safe file types only).</p>
          <p>If you need the full original ZIP (may be blocked by some mail providers), please download from Jenkins artifacts: <a href='${artifactUrl}'>Artifacts</a></p>
        """

        // Attach patterns: html inside AutomationReports folder + sanitised zip
        // Note: Some email-ext plugin versions use 'attachmentsPattern' or 'attachPatterns'.
        // We'll pass attachmentsPattern which is commonly supported.
        // Replace 'gmail-creds' with your real credential id if different.
        emailext(
          to: 'suryarajan.selvarajan@gmail.com',
          subject: subject,
          body: body,
          mimeType: 'text/html',
          attachmentsPattern: 'AutomationReports/TestAutomationReport.html, AutomationReports_safe.zip',
          from: 'suryarajan.selvarajan@gmail.com',
          credentialsId: 'gmail-creds'
        )
      }
    }

    success { echo "✅ Build passed" }
    unstable { echo "⚠️ Build unstable (tests failed)" }
    failure { echo "❌ Build failed" }
  }
}
