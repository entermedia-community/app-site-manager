jQuery(document).ready(function(url,params) 
{ 
	var apphome = $('#application').data('home') + $('#application').data('apphome');
	
	$('.taskcard').hover(
  function () {
    $(this).find(".dragicon").show();
  }, 
  function () {
    $(this).find(".dragicon").hide();
  }
);
	
	if( jQuery.fn.draggable )
	{
		jQuery(".ui-draggable").livequery( 
			function()
			{	
				jQuery(this).draggable( 
					{ 
						helper: function()
						{
							var cloned = $(this).clone();
							//TODO: Make transparent and remove white area
							return cloned;
						}
						,
						revert: 'invalid'
					}
				);
			}
		);
	}
	//categorydroparea
	if( jQuery.fn.droppable )
	{
		console.log("droppable");
		
    	jQuery(".categorydroparea").livequery(
			function()
			{
				outlineSelectionCol = function(event, ui)
				{
					jQuery(this).addClass("selected");
					jQuery(this).addClass("dragoverselected");
				}
					
				unoutlineSelectionCol = function(event, ui)
				{
					jQuery(this).removeClass("selected");
					jQuery(this).removeClass("dragoverselected");
				}
			
				jQuery(this).droppable(
					{
						drop: function(event, ui) 
						{
								console.log("dropped");
						
							var goalid = ui.draggable.data("goalid"); //Drag onto a category
							var node = $(this);
							var categoryid = node.parent().data("nodeid");
							jQuery.get(apphome + "/project/goals/drop/addtocategory.html", 
									{
										goalid:goalid,
										categoryid:categoryid,
									},
									function(data) 
									{
										node.append("<span class='fader'>&nbsp;+" + data + "</span>");
										node.find(".fader").fadeOut(3000);
										node.removeClass("selected");
										//TODO: Also update the goal card?
									}
							);
						},
						tolerance: 'pointer',
						over: outlineSelectionCol,
						out: unoutlineSelectionCol
					}
				);
			}
		);
		} //droppable
	
	//handle drag and drop priority
	/*
	jQuery('.projectgoals').livequery(function()
	{
		var sortable = jQuery(this);
		var path = sortable.data("savepath");
		
		sortable.sortable({
		    update: function (event, ui) 
		    {
		        var data = sortable.sortable('serialize');
		        data = replaceAll(data,"viewid[]=","|");
		        data = replaceAll(data,"&","");
		        data = data.replace("|","");
		        var args = {};
		        args.items = data;
		        args.viewpath = sortable.data("viewpath");
		        args.searchtype = sortable.data("searchtype");
		        args.assettype = sortable.data("assettype");
		        args.viewid = sortable.data("viewid");
		        jQuery.ajax({
		            data: args,
		            type: 'POST',
		            url: path 		            
		        });
		    },
	        stop: function (event, ui) 
	        {
	            //db id of the item sorted
	            //alert(ui.item.attr('plid'));
	            //db id of the item next to which the dragged item was dropped
	            //alert(ui.item.prev().attr('plid'));
	        }
	     });   
	});
	*/
	
	$("#commentsave").livequery("click",function()
	{
		var comment = $(this);
		var path = comment.data("savepath");
		var taskid = comment.data("taskid");
		var params = comment.data();
		params['comment'] = $("#commenttext").val();
		
		jQuery.get(path, params, function(data) 
		{
			$("#commentsarea_"+ taskid).html(data);
		});
					
	});
	
	$(".changetaskstatus").livequery(function()
	{
		var div = $(this);
		var select = div.find("select");
		select.on("change",function()
		{
			var path = div.data("savepath");
			var params = {}; //div.data();
			params['taskstatus'] = select.val();
			params['taskid'] = div.data("taskid");
			
			jQuery.get(path, params, function(data) 
			{
				div.replaceWith(data);
			});
		});
					
	});
	
});
