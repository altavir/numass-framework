<!DOCTYPE html>
<html>
<head>
    <title>${loaderName}</title>
    <meta charset="UTF-8">

    <!-- Bootstrap -->
    <link href="css/bootstrap.min.css" rel="stylesheet">

    <script type="text/javascript" src="https://www.gstatic.com/charts/loader.js"></script>
    <script type="text/javascript">
        google.charts.load('current', {'packages':['table','corechart']});
        google.charts.setOnLoadCallback(startUpdate);

        function startUpdate() {
            var opts = {sendMethod: 'auto'};
//            alert('sending request to ${dataSource}')
            var query = new google.visualization.Query("${dataSource}", opts);
            query.setRefreshInterval(${updateInterval});
            query.send(handleQueryResponse);
        }

        function handleQueryResponse(response) {
            if (response.isError()) {
                google.visualization.errors.addError.addErrorFromQueryResponse(document.getElementById('table_div'),response);
                alert('Error in query: ' + response.getMessage() + ' ' + response.getDetailedMessage());
                return;
            }

            var data = response.getDataTable();
            var table = new google.visualization.Table(document.getElementById('table_div'));

            var tableOptions = {
                showRowNumber: false,
                page: 'enable',
                width : '90%',
                pageSize: 50,
                sort : 'disable'
            };

            table.draw(data, tableOptions);

        <#if  plotParams??>
            var options = ${plotParams};

            var chart = new google.visualization.LineChart(document.getElementById('plot_div'));
            chart.draw(data, options);
        </#if>
        }
    </script>
</head>
<body>
<div class="container">
    <div class="page-header">
        <h1>Point loader: ${loaderName}</h1>
    </div>
    <br/>
    <div id="plot_div">

    </div>
    <br/>
    <div id="table_div">
        <h2>Loading data...</h2>
    </div>
</div>

<!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
<!-- Include all compiled plugins (below), or include individual files as needed -->
<script src="js/bootstrap.min.js"></script>
</body>
</html>
