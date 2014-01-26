# List comprehension
# source: http://wiki.tcl.tk/3146

# Synopsis:
# listc expression vars1 <- list1 [.. varsN <- listN] [condition].
#
# example: listc {$i * $i} i <- {1 2 3 4 5} {$i % 2 == 0} => 4 16
proc listc {expression var1 <- list1 args} {
    set res {}
  
    # Conditional expression (if not supplied) is always 'true'.
    set condition {expr 1}
  
    # We should at least have one var/list pair.
    lappend var_list_pairs  $var1 $list1
  
    # Collect any additional var/list pairs.
    while {[llength $args] >= 3 && [lindex $args 1] == "<-"} {
        lappend var_list_pairs [lindex $args 0]
        # skip "<-"
        lappend var_list_pairs [lindex $args 2]
        set args [lrange $args 3 end]
    }
 
  
    # Build the foreach commands (for each var/list pair).
    foreach {var list} $var_list_pairs {
        append foreachs [string map [list \${var} [list $var] \${list} \
            [list $list]] "foreach \${var} \${list} \{
            "]
    }

    # Remaining args are conditions
    # Insert the conditional expression.
    append foreachs [string map [list \${conditions} [list $args] \
        \${expression} [list $expression]] {

        set discard 0
        foreach condition ${conditions} {
            if !($condition) {
                set discard 1
                break
            }
        }
        if {!$discard} {
            lappend res [expr ${expression}]
        }
    }]

    
    # For each foreach, make sure we terminate it with a closing brace.
    foreach {var list} $var_list_pairs {
        append foreachs \}
    }
  
    # Evaluate the foreachs...
    eval $foreachs
    return $res
} 
