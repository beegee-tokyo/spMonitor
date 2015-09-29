<?php
	include("fusioncharts.php");
?>
<html>
	<head>
		<title>spMonitor Test</title>
		<script type="text/javascript" src="fusioncharts/fusioncharts.js"></script>
		<script type="text/javascript" src="fusioncharts/themes/fusioncharts.theme.zune.js"></script>
		<!--
		.scroll{
			width:auto;
			height:10px;
			overflow:auto;
		}
		-->
		<meta http-equiv="refresh" content="60">
	</head>

	<body bgcolor="#000000">
	<font color="white">
		<?php
			$daySelected = $_GET['day'];
			$plotType = $_GET['type'];

			/*
			 * Get list of available databases
			 */
			$dbAvail = glob("/mnt/sda1/1*.db");
			$dbAvail = array_reverse($dbAvail);

			$columnCount = 0;

			if (empty($daySelected)) {
				echo "<table><tr>";
				for ($fileNum=0; $fileNum<count($dbAvail); $fileNum++) {
					// Specify your sqlite database name and path
					$dir = 'sqlite:'.$dbAvail[$fileNum];
					/*
					 * Connect to SQLite database returning results as JSON
					 * START SQLite Section
					 */
					// Instantiate PDO connection object and failure msg
					$dbh = new PDO($dir) or die("cannot open database");

					$daysAvail = array();
					// Define your SQL statement to get available days per month per year
					$query = "SELECT DISTINCT SUBSTR(d,7,2) d FROM s WHERE d LIKE '" . substr($dir,17,5) . "%'";
					$sth = $dbh->query($query);
					$day = 0;
					echo "<td>";
					echo "<font color=\"white\">";
					print_r (substr($dir,17,5));
					echo "<font color=\"black\">";
					echo "<div style=\"overflow: auto; max-height: 100px; width: 300px; float: top; background: lightgrey; padding-left: 10px; margin: 30px\">";
					echo "<table>";
					while($row = $sth->fetch(PDO::FETCH_ASSOC)) {
						$daysAvail[] = $row;
						$fileName=substr($dir,17,5)."-".$daysAvail[$day][d];
						$recordsAvail[$day] = $fileName;
						$day++;
						echo "<tr><td><a href='index2.php?day="
								.$fileName
								."&type=MSArea'>"
								.$fileName." filled </a></td><td style=\"padding-left: 40px\">"
								."<a href='index2.php?day=".$fileName
								."&type=zoomline'>"
								.$fileName
								." zoomable </a></td></tr>";
					}
					echo "</table>";
					echo "</div>";
					echo "</td>";
					$columnCount++;
					if ($columnCount == 3) {
						$columnCount = 0;
						echo "</tr>";
						echo "<tr>";
					}
				}
				echo "</tr></table>";
			} else {
				$currDay = new DateTime(substr($daySelected,0,8));
				$lastDay = $currDay->sub(new DateInterval('P1D'));
				$currDay = new DateTime(substr($daySelected,0,8));
				$nextDay = $currDay->add(new DateInterval('P1D'));
				$lastLink = 'index2.php?day='.$lastDay->format('y-m-d').'&type='.$plotType;
				$nextLink = 'index2.php?day='.$nextDay->format('y-m-d').'&type='.$plotType;
				$currDay = new DateTime(substr($daySelected,0,8));
				$currentLink = 'index2.php?day='.$currDay->format('y-m-d');

				echo '<center><table><tr><td>';
				if (file_exists("/mnt/sda1/".$lastDay->format('y-m').".db")) {
					echo '<a href="'.$lastLink.'"><input type="button" value="'.$lastDay->format('y-m-d').'" style="background-color:#FF8C00"/></a>';
				}
				echo '</td><td>';

				echo '<a href="index2.php"><input type="button" value="Return to selection" style="background-color:#FF8C00"/></a>';
				echo '</td><td>';

				if (file_exists("/mnt/sda1/".$nextDay->format('y-m').".db")) {
					echo '<a href="'.$nextLink.'"><input type="button" value="'.$nextDay->format('y-m-d').'" style="background-color:#FF8C00"/></a>';
				}
				echo '</td><td>';

				if ($plotType=="MSArea") {
					echo '<a href="'.$currentLink.'&type=zoomline"><input type="button" value="Switch to zoomable" style="background-color:#FF8C00"/></a>';
				} else {
					echo '<a href="'.$currentLink.'&type=MSArea"><input type="button" value="Switch to filled" style="background-color:#FF8C00"/></a>';
				}

				echo '</td></tr></table></center>';

				// Specify your sqlite database name and path
   				$dir = "sqlite:/mnt/sda1/".substr($daySelected,0,5).".db";

				// Instantiate PDO connection object and failure msg
				$dbh = new PDO($dir) or die("cannot open database");

               	$timeStamps = array();
               	$solarRecords = array();
               	$consRecords = array();
               	$lightRecords = array();
               	$solarSum = (double) 0.0;
               	$consSum = (double) 0.0;
               	$solarSumMax = (double) 0.0;
               	$consSumMax = (double) 0.0;
				// Define your SQL statement to get time stamps
                $query = "SELECT * FROM s WHERE d LIKE '" . substr($daySelected,0,8) . "%'";
				$sth = $dbh->query($query);
				$index = 0;
				while($row = $sth->fetch(PDO::FETCH_ASSOC)) {
                	$json[] = $row;
                	array_push($timeStamps,$json[$index][d]);
                	array_push($solarRecords,$json[$index][s]);
                	array_push($consRecords,$json[$index][c]);
                	array_push($lightRecords,$json[$index][l]);
                	$solarSum = $solarSum + (abs((float)$json[$index][s])/60/1000);
                	$consSum = $consSum + (abs((float)$json[$index][c])/60/1000);
                	$solarVal = abs((float)$json[$index][s]);
                	$consVal = abs((float)$json[$index][c]);
                	if ($solarVal > $solarSumMax) {
                		$solarSumMax = $solarVal;
                	}
                	if ($consVal > $consSumMax) {
                		$consSumMax = $consVal;
                	}
                	$index++;
				}
				if ($index == 0) {
					echo "<font color=\"white\">";
					echo '<center><p style="background-color:#FF8C00">No data for '.substr($daySelected,0,8).'</p></center>';
					echo "<font color=\"black\">";
				} else {
					$jsonData = "{\"chart\": {
						\"caption\": \"Solar Panel Monitor\",
						\"subCaption\": \"20".substr($daySelected,0,8)."\",
						\"xAxisName\": \"Time\",
						\"yAxisName\": \"Watt\",
						\"connectNullData\": \"0\",
						\"adjustDiv\": \"0\",
						\"captionFontSize\": \"14\",
						\"subcaptionFontSize\": \"14\",
						\"subcaptionFontBold\": \"0\",
						\"paletteColors\": \"#FF8C00,#00008B\",
						\"bgcolor\": \"#FFFFFF\",
						\"canvasBgColor\": \"#AAAAAA\",
						\"legendItemFontSize\": \"20\",
						\"legendBgColor\": \"#DDDDDD\",
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
						\"showValues\": \"0\"
					},";

					$jsonData = $jsonData . "\"categories\": [ { \"category\": [";

					for ($index=0; $index<count($timeStamps); $index++) {
						$jsonData = $jsonData . "{ \"label\": \"" . substr($timeStamps[$index],9,5) . "\" },";
					}

					$jsonData = $jsonData . " ] } ],";
					$jsonData = $jsonData . "\"dataset\": [ { \"seriesname\": \"Solar\", \"data\": [";

					for ($index=0; $index<count($solarRecords); $index++) {
						$jsonData = $jsonData . "{ \"value\": \"" . $solarRecords[$index] . "\" },";
					}

					$jsonData = $jsonData . " ] },";
					$jsonData = $jsonData . "{ \"seriesname\": \"Consumption\", \"data\": [";

					for ($index=0; $index<count($consRecords); $index++) {
						$jsonData = $jsonData . "{ \"value\": \"" . $consRecords[$index] . "\" },";
					}

					$jsonData = $jsonData . " ] } ] }";

					if (!($plotType=="MSArea") && !($plotType=="zoomline")) {
						$plotType = "MSArea";
					}
					$columnChart = new FusionCharts($plotType, "spm1" , "100%", 500, "chartContainer", "json", $jsonData);

					// Render the chart

					$columnChart->render();
					echo '<table align="center", width="100%">';
					echo '<tr><td bgcolor="#DDDDDD", align="center"><font color="#FF8C00"><span style="font-weight:bold">Solar production</span></font></td><td bgcolor="#DDDDDD", align="center"><font color="#00008B"><span style="font-weight:bold">Consumption</span></font></td></tr>';
					echo '<tr><td bgcolor="#DDDDDD", align="center"><font color="#FF8C00">'.round($solarSum,3).'kWh</font></td><td bgcolor="#DDDDDD", align="center"><font color="#00008B">'.round($consSum,3).'kWh</font></td></tr>';
					echo '<tr><td bgcolor="#DDDDDD", align="center"><font color="#FF8C00"><span style="font-weight:bold">Solar peak</span></font></td><td bgcolor="#DDDDDD", align="center"><font color="#00008B"><span style="font-weight:bold">Consumption peak</span></font></td></tr>';
					echo '<tr><td bgcolor="#DDDDDD", align="center"><font color="#FF8C00">'.round($solarSumMax).'W</font></td><td bgcolor="#DDDDDD", align="center"><font color="#00008B">'.round($consSumMax).'W</font></td></tr>';
					echo '</table>';
				}
			}
		?>

		<div id="chartContainer"> </div>
	</font>
	</body>
</html>
