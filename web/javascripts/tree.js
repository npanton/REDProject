var labelType, useGradients, nativeTextSupport, animate, st;


function initTree(){
    //init data
    //end
    //init Spacetree
    //Create a new ST instance
    st = new $jit.ST({
        orientation: 'left', 
        align:"center", 
        //id of viz container element
        injectInto: 'infovis',
        //set duration for the animation
        duration: 200,
        //set animation transition type
        transition: $jit.Trans.Quart.easeInOut,
        //set distance between node and its children
        levelDistance: 25,
        levelsToShow: 20,
        //enable panning
        Navigation: {
            enable:true,
            panning:true
        },
        //set node and edge styles
        //set overridable=true for styling individual
        //nodes or edges
        Node: {
            height: 14,
            width: 45,
            type: 'rectangle',
            color: '#aaa',
            overridable: true
        },
        
        Edge: {
            type: 'bezier',
            overridable: true
        },
        
        // onBeforeCompute: function(node){
        //     Log.write("loading " + node.name);
        // },
        // 
        // onAfterCompute: function(){
        //     Log.write("done");
        // },
        
        //This method is called on DOM label creation.
        //Use this method to add event handlers and styles to
        //your node.
        onCreateLabel: function(label, node){
                
//                label.innerHTML = node.name;
            if(node.id.substring(0, node.id.indexOf("-")) == "Root"){
//                label.id = node.id;
                                label.innerHTML = json.size;

            }

            else{
                             label.innerHTML = node.name;
            }
		         
            // label.innerHTML = node.name;
            // label.innerHTML = codeLatLng(getLatLngCenter(findNode(node.id)["hull"]));

            label.onclick = function(){
             	st.onClick(node.id);
                treeToMap(node.id);
            };
            //set label styles
            var style = label.style;
            style.width = 45 + 'px';
            style.height = 50 + 'px';            
            style.cursor = 'pointer';
            style.color = '#333';
            style.fontSize = '0.8em';
            style.textAlign= 'center';
            style.paddingTop = '0px';
        },
        
        //This method is called right before plotting
        //a node. It's useful for changing an individual node
        //style properties before plotting it.
        //The data properties prefixed with a dollar
        //sign will override the global node style properties.
        onBeforePlotNode: function(node){
            //add some color to the nodes in the path between the
            //root node and the selected node.
            if (node.selected) {
                node.data.$color = "#ff7";
            }
            else {
                delete node.data.$color;
                //if the node belongs to the last plotted level
                if(!node.anySubnode("exist")) {
                    //assign a node color based on
                    //how many children it has
                    node.data.$color = '#aaa'; 
                    //['#aaa', '#baa', '#caa', '#daa', '#eaa', '#faa'][count]                 
                }
            }
        },
        
        //This method is called right before plotting
        //an edge. It's useful for changing an individual edge
        //style properties before plotting it.
        //Edge data proprties prefixed with a dollar sign will
        //override the Edge global style properties.
        onBeforePlotLine: function(adj){
            if (adj.nodeFrom.selected && adj.nodeTo.selected) {
                adj.data.$color = "#eed";
                adj.data.$lineWidth = 3;
            }
            else {
                delete adj.data.$color;
                delete adj.data.$lineWidth;
            }
        }
    });
    //load json data
    st.loadJSON(json);
    //compute node positions and layout
    st.compute();
    //optional: make a translation of the tree
    st.geom.translate(new $jit.Complex(-200, 0), "current");
    //emulate a click on the root node.
    st.onClick(st.root);
    //end

}