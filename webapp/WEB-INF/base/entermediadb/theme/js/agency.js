

jQuery(document).ready(function() 
		{ 
			$('#contactForm input[type=submit]').attr('disabled', 'disabled');
			$.validator.setDefaults({
			    errorPlacement: function(error, element) {
			    	var elementid = element.attr('id');
			    	var elementparent = $("#" + $.escapeSelector(elementid)).closest(".form-group");
			    	if(elementparent.length != 0) {
			    		error.insertAfter(elementparent);
			    	}
			    	
			    	
			    }
			});
			$("#contactFormFooter").validate({
				ignore: ".ignore",
			  rules: {
				  contactFormgrecaptcha: {
					 required: function() {
						 if(grecaptcha.getResponse() == '') {
							 return true;
						 } else {
							 return false;
						 }
					 }
				}
			  },
			  messages: {
				  contactFormgrecaptcha: {
				  remote: "Solve the Capcha."
				}
			  },
			  submitHandler: function(form){
		        $('form input[type=submit]').attr('disabled', 'disabled');
		        form.submit();
				}
			});
		
		});