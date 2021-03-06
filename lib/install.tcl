#!/usr/bin/env tclsh86

#!/home/nico/bin/tclsh

# install package ndv under tcl lib directory
set package_name ndv
set package_version 0.1.1

# history
# version date     notes
# 0.1              initial version with logger, htmlhelper and xmlhelper
# 0.1.1   8-1-2010 logger: added set_log_level_all

# 16-1-2010 niet meer doen, gebruik vaste pkgIndex.tcl
#pkg_mkIndex . *.tcl

# lib_root D:/DEVELOP/TCL85/lib/tcl8.5 => D:/DEVELOP/TCL85/lib
proc main {} {
  global package_name package_version
  
  # 9-6-2014 also to dropbox
  # 22-8-2014 first to dropbox, otherwise output might be confusing.
  install_to_dir [file join [get_dropbox_dir] install tcl lib]  

  set lib_root [file dirname [info library]]
  set lib_install [file join $lib_root "$package_name-$package_version"]
  install_to_dir $lib_install

}

proc install_to_dir {lib_install} {
  copy_dir $lib_install .
  copy_dir $lib_install db
  copy_dir $lib_install js *
}

proc get_dropbox_dir {} {
  global tcl_platform
  if {$tcl_platform(platform) == "unix"} {
    file normalize [file join ~ Dropbox]  
  } else {
    return "c:/nico/Dropbox" 
  }
}

proc copy_dir {lib_install subdir {pattern *.tcl}} {
  file mkdir [file join $lib_install $subdir]
  foreach filename [glob -directory $subdir $pattern] {
    puts "copy $filename => $lib_install/$subdir"
    file copy -force $filename [file join $lib_install $subdir]
  }
}

main
