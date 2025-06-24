package com.jpmc.midascore.kafka;


import com.jpmc.midascore.config.RestTemplateConfig;
import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Incentive;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.repository.TransactionRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


@Service
public class KafkaConsumer {
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final RestTemplate restTemplate;

    public KafkaConsumer(UserRepository userRepository, TransactionRepository transactionRepository, RestTemplate restTemplate) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.restTemplate = restTemplate;
    }

    @KafkaListener(topics = "${general.kafka-topic}", id = "transactions-group-1")
    public void consume(Transaction transaction) {
        logger.info("Consumed message: " + transaction);

        try{
            UserRecord sender = userRepository.findById(transaction.getSenderId());
            UserRecord recipient = userRepository.findById(transaction.getRecipientId());
            if (sender == null || recipient == null) {
                throw new Exception("Sender or recipient not found");
            }
            if (sender.getBalance() < transaction.getAmount()) {
                throw new Exception("Insufficient balance");
            }
            logger.info("Sender {} sending {} to Recipient {}", sender, transaction.getAmount(), recipient);

            String url = "http://localhost:8080/incentive";
            Incentive incentive = restTemplate.postForObject(url, transaction, Incentive.class);
            if (incentive == null) {
                throw new Exception("Incentive not found");
            }
            logger.info("Incentive amount {}", incentive.getAmount());

            if (incentive.getAmount() < 0){
                throw new Exception("Insufficient amount");
            }

            sender.setBalance(sender.getBalance() - transaction.getAmount());
            recipient.setBalance(recipient.getBalance() + transaction.getAmount() + incentive.getAmount());

            userRepository.save(sender);
            userRepository.save(recipient);

            TransactionRecord transactionRecord = new TransactionRecord(sender, recipient, transaction.getAmount(), incentive.getAmount());
            transactionRepository.save(transactionRecord);

            logger.info("Updated balances: Sender {} has {}, Recipient {} has {}",
                    sender, sender.getBalance(), recipient, recipient.getBalance());

        } catch (Exception e){
            logger.error("Error processing transaction: {}", e.getMessage());
        }

        logger.info("wilbur amount {}", userRepository.findById(9).getBalance());
    }
}


