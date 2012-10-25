// ===================================================================
// Author: Matt Kruse <matt@mattkruse.com>
// WWW: http://www.mattkruse.com/
//
// NOTICE: You may use this code for any purpose, commercial or
// private, without any further permission from the author. You may
// remove this notice from your final code if you wish, however it is
// appreciated by the author if at least my web site address is kept.
//
// You may *NOT* re-distribute this code in any way except through its
// use. That means, you can include it in your product, or your web
// site, or any other form where the code is actually being used. You
// may not put the plain javascript up on your site for download or
// include it in your javascript libraries for download. 
// If you wish to share this code with others, please just point them
// to the URL instead.
// Please DO NOT link directly to my .js files from your site. Copy
// the files to your server and use them there. Thank you.
// ===================================================================

// HISTORY
// ------------------------------------------------------------------
// December 9, 2003: Added script to the Javascript Toolbox
// December 10, 2003: Added the preProcessTrees variable to allow user
//      to turn off automatic conversion of UL's onLoad
// March 1, 2004: Changed it so if a <li> has a class already attached
//      to it, that class won't be erased when initialized. This allows
//      you to set the state of the tree when painting the page simply
//      by setting some <li>'s class name as being "liOpen" (see example)
/*
This code is inspired by and extended from Stuart Langridge's aqlist code:
		http://www.kryogenix.org/code/browser/aqlists/
		Stuart Langridge, November 2002
		sil@kryogenix.org
		Inspired by Aaron's labels.js (http://youngpup.net/demos/labels/) 
		and Dave Lindquist's menuDropDown.js (http://www.gazingus.org/dhtml/?id=109)
*/

// Automatically attach a listener to the window onload, to convert the trees
addEvent(window,"load",convertTrees);

// Utility function to add an event listener
function addEvent(o,e,f){
	if (o.addEventListener){ o.addEventListener(e,f,true); return true; }
	else if (o.attachEvent){ return o.attachEvent("on"+e,f); }
	else { return false; }
}

// utility function to set a global variable if it is not already set
function setDefault(name,val) {
	if (typeof(window[name])=="undefined" || window[name]==null) {
		window[name]=val;
	}
}

// Full expands a tree with a given ID
function expandTree(treeId) {
	var ul = document.getElementById(treeId);
	if (ul == null) { return false; }
	expandCollapseList(ul,nodeOpenClass);
}

// Fully collapses a tree with a given ID
function collapseTree(treeId) {
	var ul = document.getElementById(treeId);
	if (ul == null) { return false; }
	expandCollapseList(ul,nodeClosedClass);
}

// Expands enough nodes to expose an LI with a given ID
function expandToItem(treeId,itemId) {
	var ul = document.getElementById(treeId);
	if (ul == null) { return false; }
	var ret = expandCollapseList(ul,nodeOpenClass,itemId);
	if (ret) {
		var o = document.getElementById(itemId);
		if (o.scrollIntoView) {
			o.scrollIntoView(false);
		}
	}
}

// Performs 3 functions:
// a) Expand all nodes
// b) Collapse all nodes
// c) Expand all nodes to reach a certain ID
function expandCollapseList(ul,cName,itemId) {
	if (!ul.childNodes || ul.childNodes.length==0) { return false; }
	// Iterate LIs
	for (var itemi=0;itemi<ul.childNodes.length;itemi++) {
		var item = ul.childNodes[itemi];
		if (itemId!=null && item.id==itemId) { return true; }
		if (item.nodeName == "LI") {
			// Iterate things in this LI
			var subLists = false;
			for (var sitemi=0;sitemi<item.childNodes.length;sitemi++) {
				var sitem = item.childNodes[sitemi];
				if (sitem.nodeName=="UL") {
					subLists = true;
					var ret = expandCollapseList(sitem,cName,itemId);
					if (itemId!=null && ret) {
						item.className=cName;
						return true;
					}
				}
			}
			if (subLists && itemId==null) {
				item.className = cName;
			}
		}
	}
}

// Search the document for UL elements with the correct CLASS name, then process them
function convertTrees() {
	setDefault("treeClass","mktree");
	setDefault("nodeClosedClass","liClosed");
	setDefault("nodeOpenClass","liOpen");
	setDefault("nodeBulletClass","liBullet");
	setDefault("nodeLinkClass","bullet");
	setDefault("preProcessTrees",true);
	if (preProcessTrees) {
		if (!document.createElement) { return; } // Without createElement, we can't do anything
		uls = document.getElementsByTagName("ul");
		for (var uli=0;uli<uls.length;uli++) {
			var ul=uls[uli];
			if (ul.nodeName=="UL" && ul.className==treeClass) {
				processList(ul);
			}
		}
	}
}

// Process a UL tag and all its children, to convert to a tree
function processList(ul) {
	if (!ul.childNodes || ul.childNodes.length==0) { return; }
	// Iterate LIs
	for (var itemi=0;itemi<ul.childNodes.length;itemi++) {
		var item = ul.childNodes[itemi];
		if (item.nodeName == "LI") {
			// Iterate things in this LI
			var subLists = false;
			for (var sitemi=0;sitemi<item.childNodes.length;sitemi++) {
				var sitem = item.childNodes[sitemi];
				if (sitem.nodeName=="UL") {
					subLists = true;
					processList(sitem);
				}
			}
			var s= document.createElement("SPAN");
			var t= '\u00A0'; // &nbsp;
			s.className = nodeLinkClass;
			if (subLists) {
				// This LI has UL's in it, so it's a +/- node
				if (item.className==null || item.className=="") {
					item.className = nodeClosedClass;
				}
				// If it's just text, make the text work as the link also
				if (item.firstChild.nodeName=="#text") {
					t = t+item.firstChild.nodeValue;
					item.removeChild(item.firstChild);
				}
				s.onclick = function () {
					this.parentNode.className = (this.parentNode.className==nodeOpenClass) ? nodeClosedClass : nodeOpenClass;
					return false;
				}
			}
			else {
				// No sublists, so it's just a bullet node
				item.className = nodeBulletClass;
				s.onclick = function () { return false; }
			}
			s.appendChild(document.createTextNode(t));
			item.insertBefore(s,item.firstChild);
		}
	}
}

/***********************************************
 * Determine browser under which we run and
 * issue a warning message.
 ***********************************************/

function checkBrowserVersion() {
  if (navigator.appName == "Microsoft Internet Explorer") {
      // document.writeln(navigator.appName + " (" + navigator.appVersion + ")");
      var version = navigator.appVersion;
      var pos = version.indexOf("MSIE ");
      if (pos>=0) version = version.substring(pos + 5);
      pos = version.indexOf(";");
      if (pos>=0) version = version.substring(0, pos);
      // document.writeln("Your version: "+version);

      document.writeln("<H2>ADVISORY</H2>");
      document.writeln("You are using Internet Explorer, which has <b>serious</b> performance issues " +
                       "loading and navigating this page." );
      if (version.substring(0)<"7") {
         document.writeln("Additionally, your version "+version+" of IE may have a bug that causes " +
                          "continuous reload of all individual images referenced by this page.")
      }
      document.writeln("<br>Use Firefox for the best browsing experience: " +
                       "<a href=\"http://www.mozilla.com\">http://www.mozilla.com</a>.");
  };
}


/***********************************************
* Show Hint script- © Dynamic Drive (www.dynamicdrive.com)
* This notice MUST stay intact for legal use
* Visit http://www.dynamicdrive.com/ for this script and 100s more.
***********************************************/
		
var horizontal_offset="9px" //horizontal offset of hint box from anchor link

/////No further editing needed

var vertical_offset="0" //horizontal offset of hint box from anchor link. No need to change.
var ie=document.all
var ns6=document.getElementById&&!document.all

function getposOffset(what, offsettype){
   var totaloffset=(offsettype=="left")? what.offsetLeft : what.offsetTop;
   var parentEl=what.offsetParent;
   while (parentEl!=null){
      totaloffset=(offsettype=="left")? totaloffset+parentEl.offsetLeft : totaloffset+parentEl.offsetTop;
      parentEl=parentEl.offsetParent;
   }
   return totaloffset;
}

function iecompattest(){
   return (document.compatMode && document.compatMode!="BackCompat")? document.documentElement : document.body
}

function clearbrowseredge(obj, whichedge){
   return 0;
   /*
   var edgeoffset=(whichedge=="rightedge")? parseInt(horizontal_offset)*-1 : parseInt(vertical_offset)*-1
   if (whichedge=="rightedge") {
       var windowedge=ie && !window.opera? iecompattest().scrollLeft+iecompattest().clientWidth-30 : window.pageXOffset+window.innerWidth-40
       dropmenuobj.contentmeasure=dropmenuobj.offsetWidth
       if (windowedge-dropmenuobj.x < dropmenuobj.contentmeasure)
           edgeoffset=dropmenuobj.contentmeasure+obj.offsetWidth+parseInt(horizontal_offset)
   } else {
      var windowedge=ie && !window.opera ? iecompattest().scrollTop+iecompattest().clientHeight-15 : window.pageYOffset+window.innerHeight-18
      dropmenuobj.contentmeasure=dropmenuobj.offsetHeight
      if (windowedge-dropmenuobj.y < dropmenuobj.contentmeasure)
          edgeoffset=dropmenuobj.contentmeasure-obj.offsetHeight
   }
   return edgeoffset
   */
}

function showhint(menucontents, obj, e, tipwidth) {
    if ((ie||ns6) && document.getElementById("hintbox")) {
        dropmenuobj=document.getElementById("hintbox")
        dropmenuobj.innerHTML=menucontents
        dropmenuobj.style.left=dropmenuobj.style.top=-500
        if (tipwidth!=""){
            dropmenuobj.widthobj=dropmenuobj.style
            dropmenuobj.widthobj.width=tipwidth
        }
        dropmenuobj.x=getposOffset(obj, "left")
        dropmenuobj.y=getposOffset(obj, "top")
        dropmenuobj.style.left=dropmenuobj.x-clearbrowseredge(obj, "rightedge")+obj.offsetWidth+"px"
        dropmenuobj.style.top=dropmenuobj.y-clearbrowseredge(obj, "bottomedge")+"px"
        dropmenuobj.style.visibility="visible"
        obj.onmouseout=hidetip
    }
}

function hidetip(e){
    dropmenuobj.style.visibility="hidden"
    dropmenuobj.style.left="-500px"
}

function createhintbox(){
    var divblock=document.createElement("div")
    divblock.setAttribute("id", "hintbox")
    document.body.appendChild(divblock)
}


if (window.addEventListener)
    window.addEventListener("load", createhintbox, false)
else if (window.attachEvent)
    window.attachEvent("onload", createhintbox)
else if (document.getElementById)
    window.onload=createhintbox



/* Fragment Highlighting 
   var fragExclude = ('header'); */

var fragHLed = '';

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
    fragHL(location.hash.substring(1));
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
				/* var evn = "fragHL('" + fragment + "')";
				var fun = new Function('e',evn);
				an.item(i).onclick = fun; */
			} 
		}
	}
}


/*
if (window.addEventListener) {
*/
    window.addEventListener("load", fragHLload, false);
    window.addEventListener("load", fragHLlink, false);
/*
} else if (window.attachEvent) {
    window.attachEvent("onload", fragHLload);
    window.attachEvent("onload", fragHLlink);
} else if (document.getElementById) {
    window.onload=function() {
        fragHLload();
        fragHLlink();
    }
};
*/
