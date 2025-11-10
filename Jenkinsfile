pipeline {
  agent any

  tools {
    maven 'MavenLocal'
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
          if (isUnix()) {
            sh '''
              set -e
              rm -f AutomationReports_safe.zip || true
              if [ -d AutomationReports ]; then
                rm -rf tmp_reports || true
                mkdir -p tmp_reports
                # copy safe files preserving relative paths
                find AutomationReports -type f \\( -iname "*.html" -o -iname "*.htm" -o -iname "*.css" -o -iname "*.js" -o -iname "*.png" -o -iname "*.jpg" -o -iname "*.jpeg" -o -iname "*.svg" \\) -print0 | xargs -0 -I{} rsync --relative "{}" tmp_reports/
                if [ -d tmp_reports ] && [ "$(find tmp_reports -type f | wc -l)" -gt 0 ]; then
                  (cd tmp_reports && zip -r ../AutomationReports_safe.zip .) || true
                else
                  echo "No safe files found inside AutomationReports - skipping safe zip creation"
                fi
                rm -rf tmp_reports
              else
                echo "AutomationReports folder not found - skipping sanitised zip creation"
              fi
              echo "Listing workspace after zip step:"
              ls -la || true
            '''
          } else {
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
                  \$count = (Get-ChildItem -Path \$tmp -Recurse -File | Measure-Object).Count
                  if (\$count -gt 0) {
                    Compress-Archive -Path (Join-Path \$tmp '*') -DestinationPath AutomationReports_safe.zip -Force
                    Write-Output 'Created AutomationReports_safe.zip'
                  } else {
                    Write-Output 'No safe files found inside AutomationReports - skipping safe zip creation'
                  }
                  Remove-Item -Recurse -Force \$tmp
                } else {
                  Write-Output 'AutomationReports directory not present - skipped sanitised zip'
                }
                Write-Output 'Listing workspace after zip step:'
                Get-ChildItem -Force -Recurse -Depth 1
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
        archiveArtifacts artifacts: 'AutomationReports_safe.zip, AutomationReports/**', allowEmptyArchive: true, fingerprint: true
      }
    }
  }

  post {
    always {
      script {
        // debug: print the workspace and files so you can confirm what's present in console log
        echo ">>> Debug workspace listing (top-level)"
        if (isUnix()) {
          sh 'ls -la || true'
        } else {
          bat 'dir /a'
        }

        // build dynamic attachments list only with files that exist
        def attaches = []
        if (fileExists('AutomationReports_safe.zip')) {
          // check non-zero size
          def size = 0
          if (isUnix()) {
            size = sh(returnStdout: true, script: "stat -c%s AutomationReports_safe.zip || echo 0").trim().toInteger()
          } else {
            // powershell to get length
            def out = bat(returnStdout: true, script: 'powershell -NoProfile -Command "(Get-Item .\\AutomationReports_safe.zip).Length" || echo 0').trim()
            size = out.isInteger() ? out.toInteger() : (out == '' ? 0 : out.toInteger())
          }
          if (size > 0) {
            attaches << 'AutomationReports_safe.zip'
            echo "AutomationReports_safe.zip present (size=${size}) - will attach"
          } else {
            echo "AutomationReports_safe.zip exists but is zero bytes - ignoring"
          }
        } else {
          echo "AutomationReports_safe.zip not found"
        }

        if (fileExists('AutomationReports/TestAutomationReport.html')) {
          attaches << 'AutomationReports/TestAutomationReport.html'
          echo "TestAutomationReport.html found - will attach"
        } else {
          echo "TestAutomationReport.html not found"
        }

        // If safe zip wasn't created, create fallback zip that contains only the html (ensures at least something to attach)
        if (!attaches.contains('AutomationReports_safe.zip') && fileExists('AutomationReports/TestAutomationReport.html')) {
          echo "Creating fallback zip with TestAutomationReport.html as AutomationReports_safe_fallback.zip"
          if (isUnix()) {
            sh '''
              rm -f AutomationReports_safe_fallback.zip || true
              (cd AutomationReports && zip -r ../AutomationReports_safe_fallback.zip TestAutomationReport.html) || true
              ls -la AutomationReports_safe_fallback.zip || true
            '''
          } else {
            bat '''
              if exist AutomationReports_safe_fallback.zip del /F /Q AutomationReports_safe_fallback.zip
              powershell -NoProfile -Command "
                if (Test-Path 'AutomationReports\\TestAutomationReport.html') {
                  Compress-Archive -Path 'AutomationReports\\TestAutomationReport.html' -DestinationPath AutomationReports_safe_fallback.zip -Force
                  Write-Output 'Created fallback zip'
                } else {
                  Write-Output 'No HTML to zip for fallback'
                }
              "
            '''
          }
          if (fileExists('AutomationReports_safe_fallback.zip')) {
            attaches << 'AutomationReports_safe_fallback.zip'
          }
        }

        // prepare email body
        def subject = "${env.JOB_NAME} #${env.BUILD_NUMBER} - ${currentBuild.currentResult}"
        def artifactUrl = "${env.BUILD_URL}artifact/"
        def body = """
          <p>Build: <a href='${env.BUILD_URL}'>${env.BUILD_URL}</a></p>
          <p>Result: ${currentBuild.currentResult}</p>
          <p>Attached: ${attaches.join(', ')} (only existing files attached). Full artifacts: <a href='${artifactUrl}'>Artifacts</a></p>
        """

        // send email with dynamically chosen attachments
        def attachPattern = attaches.size() > 0 ? attaches.join(',') : ''
        emailext(
          to: 'suryarajan.selvarajan@gmail.com',
          subject: subject,
          body: body,
          mimeType: 'text/html',
          // only pass attachmentsPattern if we have attachments (empty string sometimes behaves oddly)
          attachmentsPattern: attachPattern,
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
