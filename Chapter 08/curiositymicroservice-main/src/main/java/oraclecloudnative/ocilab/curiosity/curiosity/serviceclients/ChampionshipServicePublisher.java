package oraclecloudnative.ocilab.curiosity.curiosity.serviceclients;


import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import oraclecloudnative.ocilab.curiosity.curiosity.QueryPage;
import oraclecloudnative.ocilab.curiosity.curiosity.SentQueryPageEvent;




/*
*
*Publish QueryPage objects to OCIStreaming QueryPage Stream 
*
*/
@Slf4j
@Service
public class ChampionshipServicePublisher {

    //private String streamEndpoint;
    //private String UTF16;
    //private String streamId;
    private final KafkaProducerMyService kafkaProducerMyService;
   

    /*public ChampionshipServicePublisher(@Value("${oci.config.stream.endpoint}") final String ociConfigStreamEndpoint, 
                                        @Value("${oci.config.stream.id}") final String ociConfigStreamId,
                                        KafkaProducerMyService kafkaProducerMyService) {
        */
        
        
    public ChampionshipServicePublisher(KafkaProducerMyService kafkaProducerMyService) {

        this.kafkaProducerMyService = kafkaProducerMyService;
        //this.UTF16 = "UTF-8";
        //this.streamEndpoint = "ociConfigStreamEndpoint";
        //this.streamId = "ociConfigStreamId";

    }

/* 
    private StreamClient prepareOCICall() {
    
        log.info("Using DEFAULT profile from the default OCI configuration file ($HOME/.oci/config)");
        try {
            var ociPersonalConfigurationFile = ConfigFileReader.parseDefault();
            var ociAuthenticationProvider = new ConfigFileAuthenticationDetailsProvider(ociPersonalConfigurationFile);
            log.info("Preparing OCI API clients (for Streaming)");	
            return StreamClient.builder().endpoint(streamEndpoint).build(ociAuthenticationProvider);
        } catch (IOException e) {
            log.error("Error preparing OCI Call");		
            e.printStackTrace();
        }
           
        return null;
     }
     */

     private SentQueryPageEvent buildEvent(final QueryPage queryPage){
                return new SentQueryPageEvent(queryPage.getId(), 
                                      queryPage.getUser().getId(), 
                                      queryPage.getUser().getUserName(), 
                                      queryPage.getOriginalQuery(),
                                      queryPage.getQuery());

     }
     /* 
     public void publishMessageToStream(QueryPage queryPage)
            throws UnsupportedEncodingException {

                SentQueryPageEvent event = buildEvent(queryPage);

                StreamClient streamClient = this.prepareOCICall();

                ObjectMapper objectMapper = new ObjectMapper();
                   
                  String queryPageAsString = "init_value";
                  try {
                      queryPageAsString = objectMapper.writeValueAsString(event);
                  } catch (JsonProcessingException e) {
                       log.error("Error trying to transform object QueryPage into String with objectMapper");
                        e.printStackTrace();
                  }


        var messages = new ArrayList<PutMessagesDetailsEntry>();
						for (int i = 0; i < 1; i++) { 
							messages.add(
									PutMessagesDetailsEntry.builder()
											.key(String.format("messageKey-%s",  queryPage.getId()).getBytes(UTF16))
											.value(String.format(queryPageAsString).getBytes(UTF16))
											.build());
						}

        var putMessagesDetails = PutMessagesDetails.builder().messages(messages).build();
        
        var putMessagesRequest = PutMessagesRequest.builder()
                                    .streamId(streamId)
                                    .putMessagesDetails(putMessagesDetails)
                                    .build();
        var putMessagesResponse = streamClient.putMessages(putMessagesRequest);
        var putMessagesResponseCode = putMessagesResponse.get__httpStatusCode__();
        if(putMessagesResponseCode!=200) {
            log.error("Error in putMessagesResponse -  error{}", putMessagesResponseCode);
            System.exit(1);
        }
        log.info("Successfully published the message to the stream");
    }
    */

    public void publishMessageToKafkaString(String string) {
        // TODO: Implement code to send a message to Kafka

        log.info("Publishing message to Kafka");
        log.info(string);

        kafkaProducerMyService.sendMessage(string);  
  
    }


    public void publishMessageToKafka(QueryPage queryPage) {
        // TODO: Implement code to send a message to Kafka

        log.info("Publishing message object to Kafka");
        log.info(queryPage.toString());


        SentQueryPageEvent queryPageEvent = buildEvent(queryPage);

        kafkaProducerMyService.sendMessage2(queryPageEvent);   
  
    }
        



    

    
}
