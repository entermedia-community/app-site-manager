jQuery(document).ready(function(url,params) 
{ 
	var home = $('#application').data('home') + $('#application').data('apphome');
	
	//handle drag and drop
	jQuery('.projectgoals').livequery(function()
	{
		var sortable = jQuery(this);
		var path = sortable.data("savepath");
		
		sortable.sortable({
			axis: 'y',
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
}