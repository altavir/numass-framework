<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="utf-8">
        <meta http-equiv="refresh" content="30">
        
        <!-- Latest compiled and minified CSS -->
        <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css" integrity="sha384-1q8mTJOASx8j1Au+a5WDVnPi2lkFfwwEAa8hDDdjZlpLegxhjVME1fgjWPGmkzs7" crossorigin="anonymous">

        
        <title>Numass storage</title>
        </head>
    <style>
        .shifted { 
            margin: 20px;
        }
        </style>
    <body>
        <h1> Server configuration </h1>
        <#if serverMeta??>
            <h3> Server metadata: </h3>
            ${serverMeta}
        </#if>
        <br/>
        <#if serverRootState??>
            <h3> Current root state: </h3>
            ${serverRootState}
        </#if>            
        <br/>
        <#if runPresent>
            <h1> Current run configuration </h1>
            <#if runMeta??>
                <h3> Run metadata: </h3>
                ${runMeta}
            </#if>
            <#if runState?? >
                <h3> Current run state: </h3>
                ${runState}
            </#if>

            <h2> Current run storage content: </h2>
            ${storageContent}
        </#if>
            
        <!-- Optional theme -->
        <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap-theme.min.css" integrity="sha384-fLW2N01lMqjakBkx3l/M9EahuwpSfeNvV63J5ezn3uZzapT0u7EYsXMjQV+0En5r" crossorigin="anonymous">

        <!-- Latest compiled and minified JavaScript -->
        <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/js/bootstrap.min.js" integrity="sha384-0mSbJDEHialfmuBBQP6A4Qrprq5OVfW37PRR3j5ELqxss1yVqOtnepnHVP9aJ7xS" crossorigin="anonymous"></script>
    </body>
</html>        