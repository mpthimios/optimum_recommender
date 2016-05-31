package imu.recommender;

public class Message {

        private String messageId;
        private String messageText;
        private Double utility;

        public String getMessageId() {
            return messageId;
        }
        public void setMessageId(String messageId) {
            this.messageId = messageId;
        }
        public String getMessageText() {
            return messageText;
        }
        public void setMessageText(String messageText) {
            this.messageText = messageText;
        }
        public Double getUtility() {
            return utility;
        }
        public void setUtility(Double utility) {
            this.utility = utility;
        }
    }
