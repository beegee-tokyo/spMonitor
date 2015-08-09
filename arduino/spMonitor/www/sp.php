<?php
   class MyDB extends SQLite3
   {
      function __construct()
      {
         $this->open('/mnt/sda1/s.db');
      }
   }
   $db = new MyDB();
   if(!$db){
      echo $db->lastErrorMsg();
   } else {
      echo "Opened database successfully\n\r";
   }
   echo "\n\r";

   $sql =<<<EOF
      SELECT * from s;
EOF;

   $ret = $db->query($sql);
   $a = array();
   $d = array();
   while($row = $ret->fetchArray(SQLITE3_ASSOC) ){
      echo "TimeStamp = ". $row['d'] . " - ";
	  $d["date"] = $row['d'];
      echo "Solar = ". $row['s'] ."W - ";
	  $d["solar"] = $row['s'];
      echo "Consumption = ". $row['c'] ."W - ";
	  $d["cons"] = $row['i'];
      echo "Light =  ".$row['l'] ."lux\n\r\n\r";
	  $d["light"] = $row['l'];
	  array_push($a,$d);
   }
   echo json_encode($all);
   $db->close();
?>