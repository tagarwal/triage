/* Fragment 
   Highlighting */

var fragExclude = ('header'); 
var fragHLed = '';

Array.prototype.search = function(myVariable) { 
  for(x in this) if (x == myVariable) return true; return false;
}

/* This unhighlights any existing elements 
   that have been highlighted, then highlights the specified one.
*/
function fragHL(frag) {
    if (fragHLed.length > 0 && document.getElementById(fragHLed)) {
       KillClass(document.getElementById(fragHLed),'fragment');
    }
    if (frag.length > 0 && document.getElementById(frag)) {
       fragHLed = frag;
       AddClass (document.getElementById(frag),'fragment');
    }
}

/* called by the onload event handler,
   it extracts the value after the "#" in the requested 
   URL and calls fragHL */
function fragHLload() {
    /*
        fragHL(location.hash.substring(1));
     */
    document.write('Doc location is: ' + document.location + '<br>');
    var pos = document.location.indexOf('#');
    fragHL(document.location.sibstring(pos+1));
}

/* scans the document for links that have href 
   attributes with values starting with a "#". 
   It then adds onclick events to each to call fragHL() */
function fragHLlink() {
  if (document.getElementsByTagName) {
    var an = document.getElementsByTagName('a');
    for (i=0; i<an.length; i++) {
      if (an.item(i).getAttribute('href').indexOf('#') >= 0) {
        var fragment = an.item(i).getAttribute('href').substring(
          an.item(i).getAttribute('href').indexOf('#') + 1
        );
        if (fragExclude.search(fragment)) {
          var evn = "fragHL('" + fragment + "')";
          var fun = new Function('e',evn);
          an.item(i).onclick = fun;
        }
      } 
    }
  }
}


/*
if (window.addEventListener) {
    window.addEventListener("load", fragHLload, false);
    window.addEventListener("load", fragHLlink, false);
} else if (window.attachEvent) {
    window.attachEvent("onload", fragHLload);
    window.attachEvent("onload", fragHLlink);
} else if (document.getElementById) {
*/
    window.onload=function() {
        fragHLload();
        fragHLlink();
    }
/*
};
*/
