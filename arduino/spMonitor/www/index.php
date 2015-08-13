<?php
	include("fusioncharts.php");
?>
<html>
	<head>
		<title>spMonitor Test</title>
		<script type="text/javascript" src="fusioncharts/fusioncharts.js"></script>
		<script type="text/javascript" src="fusioncharts/themes/fusioncharts.theme.zune.js"></script>
	</head>

	<body>

		<?php
			$selectedFileName = $_GET['fileName'];

			$files = glob('/mnt/sda1/*.txt');
			foreach ($files as $file_num => $fileName) {
				echo "<a href='index.php?fileName=".$fileName."'>".substr($fileName,10,8)."</a></br>";
			}

			if (!empty($selectedFileName)) {
				$lines = file($selectedFileName);

				$jsonData = "{\"chart\": {
						\"caption\": \"Solar Panel Monitor\",
						\"subCaption\": \"20".substr($selectedFileName,10,8)."\",
						\"xAxisName\": \"Time\",
						\"yAxisName\": \"Watt\",
						\"captionFontSize\": \"14\",
						\"subcaptionFontSize\": \"14\",
						\"subcaptionFontBold\": \"0\",
						\"paletteColors\": \"FF8C00,#00008B\",
						\"bgcolor\": \"#888888\",
						\"showBorder\": \"0\",
						\"showShadow\": \"0\",
						\"plotFillAlpha\": \"60\",
						\"showCanvasBorder\": \"0\",
						\"usePlotGradientColor\": \"0\",
						\"legendBorderAlpha\": \"0\",
						\"legendShadow\": \"0\",
						\"showAxisLines\": \"0\",
						\"showAlternateHGridColor\": \"0\",
						\"divlineThickness\": \"1\",
						\"divLineIsDashed\": \"1\",
						\"divLineDashLen\": \"1\",
						\"divLineGapLen\": \"1\",
						\"showPeakData \": \"0\",
						\"maxPeakDataLimit  \": \"0\",
						\"minPeakDataLimit  \": \"0\",
						\"xAxisName\": \"Day\",
						\"showValues\": \"0\"
					},";

				$jsonData = $jsonData . "\"categories\": [ { \"category\": [";

				foreach ($lines as $line_num => $line) {
					trim($line);
					$lineValues = explode(",",$line);
					$jsonData = $jsonData . "{ \"label\": \"" . $lineValues[3] . "\" },";
				}

				rtrim($jsonData, ",");
				$jsonData = $jsonData . " ] } ],";
				$jsonData = $jsonData . "\"dataset\": [ { \"seriesname\": \"Solar\", \"data\": [";

				foreach ($lines as $line_num => $line) {
					trim($line);
					$lineValues = explode(",",$line);
					$jsonData = $jsonData . "{ \"value\": \"" . $lineValues[5] . "\" },";
				}

				rtrim($jsonData, ",");
				$jsonData = $jsonData . " ] },";
				$jsonData = $jsonData . "{ \"seriesname\": \"Consumption\", \"data\": [";

				foreach ($lines as $line_num => $line) {
					trim($line);
					$lineValues = explode(",",$line);
					$consVal = abs(floatval($lineValues[6]));
					$consString = sprintf("%.3f", $consVal);
					$jsonData = $jsonData . "{ \"value\": \"" . $consString . "\" },";
				}

				rtrim($jsonData, ",");
				$jsonData = $jsonData . " ] } ] }";

				//echo $jsonData;

				$columnChart = new FusionCharts("zoomline", "spm1" , "100%", 500, "chartContainer", "json", $jsonData);

				// Render the chart

				$columnChart->render();
			}
		?>

		<div id="chartContainer">spMonitor plot will load here!</div>
	</body>
</html>
