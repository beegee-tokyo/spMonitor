.mode csv
-- use '.separator SOME_STRING' for something other than a comma.
.headers on
.out s.csv
select * from s;
