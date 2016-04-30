<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="utf-8">
        <meta http-equiv="refresh" content="30">
        
        <!-- Bootstrap -->
        <link href="css/bootstrap.min.css" rel="stylesheet">
        
        <title>Numass storage</title>
        </head>
        <style>
            .shifted { 
                margin: 20px;
            }
        </style>
    <body>
        <div class="container">
            <div class="page-header">
                <h1> Server configuration </h1>
            <div/>
            <#if serverMeta??>
                <div class="container">
                    <h3> Server metadata: </h3>
                    <div class="well well-lg">
                        ${serverMeta}
                    </div>
                </div>
            </#if>
            <br/>
            <#if serverRootState??>
                <div class="container">
                    <h3> Current root state: </h3>
                    <div class="well well-lg">
                        ${serverRootState}
                    </div>
                </div>
            </#if>            
            <br/>
            <#if runPresent>
                <h2> Current run configuration </h2>
                <#if runMeta??>
                    <div class="container">
                        <h3> Run metadata: </h3>
                        <div class="well well-lg">
                            ${runMeta}
                        </div>
                    </div>
                </#if>
                <#if runState?? >
                    <div class="container">
                        <h3> Current run state: </h3>
                        <div class="well well-lg">
                            ${runState}
                        </div>
                    </div>
                </#if>

                <h3> Current run storage content: </h3>
                <div class="well well-lg">
                    ${storageContent}
                </div>
            </#if>
        </div>
            
        <!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
        <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
        <!-- Include all compiled plugins (below), or include individual files as needed -->
        <script src="js/bootstrap.min.js"></script>    
    </body>
</html>        