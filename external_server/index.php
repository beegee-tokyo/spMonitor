<?php
	include("fusioncharts.php");
	include ("utility.php");

	function leapYear($testYear){ 
		if(($testYear% 400) == 0 || (($testYear% 4) == 0 && ($testYear% 100) != 0)) 
			return "1"; 
		else 
			return "0"; 
	}

	function subDay($year, $month, $day){
		if ($day == "01") { // first day of month
			switch ($month) {
 				case "05": // month before has 30 days
					$month = "04";
					$day = "30";
					break;
 				case "07": // month before has 30 days
					$month = "06";
					$day = "30";
					break;
 				case "10": // month before has 30 days
					$month = "09";
					$day = "30";
					break;
 				case "12": // month before has 30 days
					$month = "11";
					$day = "30";
					break;
				case "01": // month before has different year
					$month = "12";
					$yearInt = (int)("20".$year);
					$yearInt -= 2001;
					$year = str_pad($yearInt, 2, '0', STR_PAD_LEFT);
					$day = "31";
					break;
				case "08": // month before has 31 days
					$month = "07";
					$day = "31";
					break;
				case "02": // month before has 31 days
					$month = "01";
					$day = "31";
					break;
				case "04": // month before has 31 days
					$month = "03";
					$day = "31";
					break;
				case "06": // month before has 31 days
					$month = "05";
					$day = "31";
					break;
				case "09": // month before has 31 days
					$month = "08";
					$day = "31";
					break;
				case "11": // month before has 31 days
					$month = "10";
					$day = "31";
					break;
				case "03": //month before has 28 or 29 days
					$month = "02";
					$yearInt = ((int)$year)+2000;
					if (leapYear($yearInt) == "1") {
						$day = "29";
					} else {
						$day = "28";
					}
					break;
			}
		} else { // Just sub a day
			$dayInt = (int)($day);
			$dayInt -= 1;
			$day = str_pad($dayInt, 2, '0', STR_PAD_LEFT);
		}
		return $year."-".$month."-".$day;
	}

	function addDay($year, $month, $day){
		$yearInt = ((int)$year)+2000;
		$isLeapYear = leapYear($yearInt);
		if ($day == "31") { // months with 31 days and last day
			$day = "01";
			$monthInt = (int)($month);
			$monthInt += 1;
			$month = str_pad($monthInt, 2, '0', STR_PAD_LEFT);
			return $year."-".$month."-".$day;
		} 
		if ($day == "30" && ($month == "04" || $month == "06" || $month == "09" || $month == "11")) { // months with 30 days and last day
			$day = "01";
			$monthInt = (int)($month);
			$monthInt += 1;
			$month = str_pad($monthInt, 2, '0', STR_PAD_LEFT);
			return $year."-".$month."-".$day;
		} 
		if ($day == "29" && $month == "02") { // february in leap-year and last day
			$day = "01";
			$monthInt = (int)($month);
			$monthInt += 1;
			$month = str_pad($monthInt, 2, '0', STR_PAD_LEFT);
			return $year."-".$month."-".$day;
		} 
		if ($day == "28" && $month == "02" && $isLeapYear == "0") { // february in none leap-year and last day
			$day = "01";
			$monthInt = (int)($month);
			$monthInt += 1;
			$month = str_pad($monthInt, 2, '0', STR_PAD_LEFT);
			return $year."-".$month."-".$day;
		}
		$dayInt = (int)($day);
		$dayInt += 1;
		$day = str_pad($dayInt, 2, '0', STR_PAD_LEFT);
		return $year."-".$month."-".$day;
	}
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

		<?php
			$daySelected = $_GET['day'];
			$plotType = $_GET['type'];
			/*
			 * Connect to SQLite database returning results as JSON
			 * START SQLite Section
			 */
			// Instantiate PDO connection object and failure msg
			$dbh = new PDO('mysql:host=localhost;dbname=giesecke_sp',  $username, $password)or die("cannot open database");
			$dbh->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

			$yearsAvail = array();
			$recordsAvail;

			if (empty($daySelected)) {
				echo "<table><tr>";
				$columnCount = 0;

				// Start drawing the table
				echo "<div style=\"overflow: auto; max-height: 100px; width: 300px; float: top; background: lightgrey; padding-left: 10px; margin: 30px\">";

				// Define your SQL statement to get available years
				$query = "SELECT DISTINCT SUBSTR(d,1,2) y FROM s ORDER BY y DESC";
				$sth = $dbh->query($query);
				while($row = $sth->fetch(PDO::FETCH_ASSOC)) {
					$yearsAvail[] = $row;
				};
				for ($year=0; $year<count($yearsAvail); $year++) {
					$monthsAvail = array();
					// Define your SQL statement to get available months per year
					$query = "SELECT DISTINCT SUBSTR(d,4,2) m FROM s WHERE d LIKE '" . $yearsAvail[$year]['y'] . "%' ORDER BY m DESC";
					$sth = $dbh->query($query);
					while($row = $sth->fetch(PDO::FETCH_ASSOC)) {
						$monthsAvail[] = $row;
					}
					for ($month=0; $month<count($monthsAvail); $month++) {
						$daysAvail = array();
						// Define your SQL statement to get available days per month per year
						$query = "SELECT DISTINCT SUBSTR(d,7,2) d FROM s WHERE d LIKE '" . $yearsAvail[$year]['y'] .
						 "-" . $monthsAvail[$month]['m'] . "%'";
						$sth = $dbh->query($query);
						$day = 0;
						echo "<td>";
                        echo "<font color=\"white\">";
                        print_r ($yearsAvail[$year][y]."-".$monthsAvail[$month][m]);
                        echo "<font color=\"black\">";
                        echo "<div style=\"overflow: auto; max-height: 100px; width: 300px; float: top; background: lightgrey; padding-left: 10px; margin: 30px\">";
                        echo "<table>";
						while($row = $sth->fetch(PDO::FETCH_ASSOC)) {
							$daysAvail[] = $row;
							$fileName=$yearsAvail[$year][y]."-".$monthsAvail[$month][m]."-".$daysAvail[$day][d];
							$recordsAvail[$day] = $fileName;
							$day++;
							echo "<tr><td><a href='index.php?day="
									.$fileName
									."&type=MSArea'>"
									.$fileName." filled </a></td><td style=\"padding-left: 40px\"><a href='index.php?day=".$fileName
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
				}
				echo "</tr></table>";
			} else {
				$currDateYear = substr($daySelected,0,2);
				$currDateMonth = substr($daySelected,3,2);
				$currDateDay = substr($daySelected,6,2);
				$lastDay = subDay($currDateYear , $currDateMonth , $currDateDay );
				$nextDay = addDay($currDateYear , $currDateMonth , $currDateDay );
            	$lastLink = 'index.php?day='.$lastDay.'&type='.$plotType;
            	$nextLink = 'index.php?day='.$nextDay.'&type='.$plotType;
            	$currentLink = 'index.php?day='.$daySelected;

            	echo '<center><table><tr><td>';
           		echo '<a href="'.$lastLink.'"><input type="button" value="'.$lastDay.'" style="background-color:#FF8C00"/></a>';
            	echo '</td><td>';

                echo '<a href="index.php"><input type="button" value="Return to selection" style="background-color:#FF8C00"/></a>';
            	echo '</td><td>';

           		echo '<a href="'.$nextLink.'"><input type="button" value="'.$nextDay.'" style="background-color:#FF8C00"/></a>';
            	echo '</td><td>';

            	if ($plotType=="MSArea") {
            		echo '<a href="'.$currentLink.'&type=zoomline"><input type="button" value="Switch to zoomable" style="background-color:#FF8C00"/></a>';
            	} else {
            		echo '<a href="'.$currentLink.'&type=MSArea"><input type="button" value="Switch to filled" style="background-color:#FF8C00"/></a>';
            	}

            	echo '</td></tr></table></center>';

               	$timeStamps = array();
               	$solarRecords = array();
               	$consRecords = array();
               	$lightRecords = array();
				$solarSum = (double) 0.0;
				$consSum = (double) 0.0;
				$solarSumMax = (double) 0.0;
				$consSumMax = (double) 0.0;
				$solarNow = (double) 0.0;
				$consNow = (double) 0.0;
				// Define your SQL statement to get time stamps
                $query = "SELECT * FROM s WHERE d LIKE '" . $daySelected . "%' ORDER BY d";
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
                	$solarNow = $solarVal;
                	$consNow = $consVal;
					$index++;
				}

                if ($index == 0) {
					echo "<font color=\"white\">";
                	echo '<center><p style="background-color:#FF8C00">No data for '.$daySelected.'</p></center>';
					echo "<font color=\"black\">";
				} else {
					$jsonData = "{\"chart\": {
						\"caption\": \"Solar Panel Monitor\",
						\"subCaption\": \"20".$daySelected."\",
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

					echo '<table align="center", width="100%">';
					echo '<tr><td bgcolor="#DDDDDD", align="center"><font color="#FF8C00"><span style="font-weight:bold">Solar production</span></font></td><td bgcolor="#DDDDDD", align="center"><font color="#00008B"><span style="font-weight:bold">Consumption</span></font></td></tr>';
					echo '<tr><td bgcolor="#DDDDDD", align="center"><font color="#FF8C00">'.round($solarSum,3).'kWh</font></td><td bgcolor="#DDDDDD", align="center"><font color="#00008B">'.round($consSum,3).'kWh</font></td></tr>';
					echo '<tr><td bgcolor="#DDDDDD", align="center"><font color="#FF8C00"><span style="font-weight:bold">Solar peak</span></font></td><td bgcolor="#DDDDDD", align="center"><font color="#00008B"><span style="font-weight:bold">Consumption peak</span></font></td></tr>';
					echo '<tr><td bgcolor="#DDDDDD", align="center"><font color="#FF8C00">'.round($solarSumMax).'W</font></td><td bgcolor="#DDDDDD", align="center"><font color="#00008B">'.round($consSumMax).'W</font></td></tr>';
					echo '<tr><td bgcolor="#DDDDDD", align="center"><font color="#FF8C00"><span style="font-weight:bold">Solar now</span></font></td><td bgcolor="#DDDDDD", align="center"><font color="#00008B"><span style="font-weight:bold">Consumption now</span></font></td></tr>';
					echo '<tr><td bgcolor="#DDDDDD", align="center"><font color="#FF8C00">'.round($solarNow).'W</font></td><td bgcolor="#DDDDDD", align="center"><font color="#00008B">'.round($consNow).'W</font></td></tr>';
					echo '</table>';

					// Render the chart
					$columnChart->render();
				}
			}
		?>

		<div id="chartContainer">spMonitor plot from database will load here!</div>
	</body>
</html>