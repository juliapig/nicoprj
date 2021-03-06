# ./nanny.tcl tclsh ./scatter2db.tcl -nopost -moveread -continuous
# ./nanny.tcl tclsh ./scatter2db.tcl -nopost -moveread -continuous -updatedaily -updatemaxitem
# ./nanny.tcl tclsh ./scatter2db.tcl -nopost -moveread -continuous -updatedaily

# 1-11-2013 nu even zonder -updatedaily, staat in de steigers.
# ./nanny.tcl tclsh ./scatter2db.tcl -nopost -moveread -continuous -actions dailystats,gt3,maxitem,analyze
# ./nanny.tcl tclsh ./scatter2db.tcl -nopost -moveread -continuous -actions dailystats,gt3,aggrsub,maxitem,slowitem,topic,domain_ip,analyze
# ./nanny.tcl tclsh ./scatter2db.tcl -nopost -moveread -continuous -actions dailystats,gt3,aggrsub,maxitem,slowitem,topic,domain_ip,aggr_specific,analyze

# new 23-12-2013 include vacuum and removeold. @todo replace actions with 'all'
# ./nanny.tcl tclsh ./scatter2db.tcl -nopost -moveread -continuous -actions dailystats,gt3,aggrsub,slowitem,topic,domain_ip,aggr_specific,removeold,analyze,vacuum

# ./nanny.tcl tclsh ./scatter2db.tcl -nopost -moveread -continuous -actions all
# ./nanny2.tcl -checkfile download-scatter.tcl.log -timeout 1800 tclsh ./download-scatter.tcl
#./nanny2.tcl -checkfile scatter2db.tcl.log -timeout 1800 tclsh ./scatter2db.tcl -nopost -moveread -continuous -actions all

# ./nanny.tcl tclsh ./scatter2db.tcl -nopost -moveread -continuous

# 8-2-2014 logfile nu dynamisch, dus andere checkfile
./nanny2.tcl -checkfile scatter2db-check.log -timeout 1800 tclsh ./scatter2db.tcl -nopost -moveread -continuous -actions all -checkfile scatter2db-check.log

