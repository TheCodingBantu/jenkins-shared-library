**Jenkins Shared Libaries**

Usage:
 - Import shared libary in your jenkinsfile
    ```
        @Library('jp-devops-shared-library') _

    ```
Methods:
readOrUpdateVersion()
 - Takes in action parameter (can be 'read' or 'update')
