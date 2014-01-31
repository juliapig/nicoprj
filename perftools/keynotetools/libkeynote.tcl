# libkeynote.tcl - generic functions for importing and handling Keynote data.

# @note also used in migrations and scatter2db.
proc det_topdomain {domain} {
  # return $domain 
  # if it's something like www.xxx.co(m).yy, then return xxx.co(m).yy
  # otherwise if it's like www.xxx.yy, then return xxx.yy
  # maybe regexp isn't the quickest, try split/join first.
  set l [split $domain "."]
  set p [lindex $l end-1]
  if {($p == "com") || ($p == "co")} {
    join [lrange $l end-2 end] "." 
  } else {
    if {$domain == "images.philips.com"} {
      return "scene7" 
    } else {
      join [lrange $l end-1 end] "."
    }
  }  
}
