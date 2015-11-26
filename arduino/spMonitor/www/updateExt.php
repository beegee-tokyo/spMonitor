<?php
	/*
	 * Get list of available databases
	 */
	$dbAvail = glob("/mnt/sda1/1*.db");
	$dbAvail = array_reverse($dbAvail);

	$columnCount = 0;

	//for ($fileNum=0; $fileNum<count($dbAvail); $fileNum++) {
	for ($fileNum=0; $fileNum<1; $fileNum++) {
		// Specify your sqlite database name and path
		print "Working file $dbAvail[$fileNum]\n";
		$dir = 'sqlite:'.$dbAvail[$fileNum];
		/*
		 * Connect to SQLite database returning results as JSON
		 * START SQLite Section
		 */
		// Instantiate PDO connection object and failure msg
		$dbh = new PDO($dir) or die("cannot open database");

		// Define your SQL statement to get time stamps
        $query = "SELECT * FROM s";
		$sth = $dbh->query($query);
		$index = 0;

               	$timeStamps = array();
               	$solarRecords = array();
               	$consRecords = array();
               	$lightRecords = array();

		while($row = $sth->fetch(PDO::FETCH_ASSOC)) {
                	$json[] = $row;
                	array_push($timeStamps,$json[$index][d]);
                	array_push($solarRecords,$json[$index][s]);
                	array_push($consRecords,$json[$index][c]);
                	array_push($lightRecords,$json[$index][l]);
                	$index++;
		}

		for ($index=0; $index<count($timeStamps); $index++) {
			/*
			 * Check if entry exist in remote mySQL
			 */
			$curlString = "sp.giesecke.tk/q.php?date=".$timeStamps[$index];
			//$curlString = "sp.giesecke.tk/q.php?date=15-12-01-00:00";
			$curl = curl_init();
			curl_setopt_array($curl, array(CURLOPT_RETURNTRANSFER => 1, CURLOPT_URL => $curlString));
			$result = curl_exec ($curl);
			curl_close ($curl);
			//print $result."\n";
			$response = json_decode(substr($result,1,-1));
			if ($response == null) {
				print $timeStamps[$index]." not found\n";
				$curlString = "sp.giesecke.tk/u.php?d=".$timeStamps[$index]."&s=".$solarRecords[$index]."&c=".$consRecords[$index]."&l=".$lightRecords[$index];
				print $timeStamps[$index]." - ";

				$curl = curl_init();
				//curl_setopt($curl,CURLOPT_URL,$curlString);
				curl_setopt_array($curl, array(CURLOPT_RETURNTRANSFER => 1, CURLOPT_URL => $curlString));
				$result = curl_exec ($curl);
				curl_close ($curl);
				print $result."\n";
			} else {
				//print $timeStamps[$index]." found\n";
			}
			//sleep(10);
		}
	}
?>
