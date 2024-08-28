#!/usr/bin/env groovy

import com.example.Docker

def call(String sl_epoUrl, String sl_branchName, String sl_credentialsId) {
    return new Docker(this).checkoutGitRepo(String sl_repoUrl, String sl_branchName, String sl_credentialsId)
}
