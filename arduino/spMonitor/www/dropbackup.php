<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
    <title>Backup Databases to DropBox</title>
</head>
<body>
<h1>Backup Databases to DropBox</h1>
<?php
    # Include the Dropbox SDK libraries
    require_once "dropbox/autoload.php";
    use \Dropbox as dbx;

    $accessToken = "LuJm2DxnRssAAAAAAAAGhog1TzKdOZSkpHy9Evsi1hxEnIp8UFs_vT37O1hlCNwI";

	/*
	 * Get list of available databases
	 */
	$dbAvail = glob("/mnt/sda1/1*.db");

	echo $dbAvail[$fileNum];
	echo "<b>Backup files:</b>";
	echo "<br />";

	for ($fileNum=0; $fileNum<count($dbAvail); $fileNum++) {
		// Specify your sqlite database name and path
		echo $dbAvail[$fileNum];
		echo " - size: ";
		$fileSize = filesize($dbAvail[$fileNum]);

        if ($fileSize >= 1073741824)
        {
            $fileSize = number_format($fileSize / 1073741824, 2) . ' GB';
        }
        elseif ($fileSize >= 1048576)
        {
            $fileSize = number_format($fileSize / 1048576, 1) . ' MB';
        }
        elseif ($fileSize >= 1024)
        {
             $fileSize = number_format($fileSize / 1024, 0) . ' KB';
        }

        echo $fileSize;
		echo "<br />";
	}

    echo "<br />";

	for ($fileNum=0; $fileNum<count($dbAvail); $fileNum++) {
		// Specify your sqlite database name and path
		$sourceFile = $dbAvail[$fileNum];
		$destinationFile = '/'.substr($dbAvail[$fileNum],10,8);

        $dbxClient = new dbx\Client($accessToken, "PHP-Example/1.0");
        $accountInfo = $dbxClient->getAccountInfo();

        $f = fopen($sourceFile, "rb");
        $result = $dbxClient->uploadFile($destinationFile, dbx\WriteMode::force(), $f);
        fclose($f);
        echo "<b>Sending result ".substr($dbAvail[$fileNum],10,8).": </b>";
        print_r($result[size]);
        echo "<br />";
	}
?>
</body>
</html>