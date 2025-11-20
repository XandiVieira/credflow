package com.relyon.credflow.service;

import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.credit_card.CreditCard;
import com.relyon.credflow.model.credit_card.CreditCardResponseDTO;
import com.relyon.credflow.model.mapper.CreditCardMapper;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.repository.AccountRepository;
import com.relyon.credflow.repository.CreditCardRepository;
import com.relyon.credflow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditCardServiceTest {

    @Mock
    private CreditCardRepository creditCardRepository;

    @Mock
    private CreditCardMapper creditCardMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CreditCardBillingService billingService;

    @Mock
    private LocalizedMessageTranslationService translationService;

    @InjectMocks
    private CreditCardService service;

    @Test
    void findAll_whenCreditCardsExist_shouldReturnPageOfDTOs() {
        var accountId = 1L;
        var page = 0;
        var size = 20;

        var card1 = CreditCard.builder()
                .id(1L)
                .nickname("Visa Gold")
                .closingDay(15)
                .dueDay(25)
                .build();
        var card2 = CreditCard.builder()
                .id(2L)
                .nickname("Mastercard Platinum")
                .closingDay(10)
                .dueDay(20)
                .build();

        var dto1 = new CreditCardResponseDTO();
        dto1.setId(1L);
        var dto2 = new CreditCardResponseDTO();
        dto2.setId(2L);

        var pageRequest = PageRequest.of(page, size);
        var cardPage = new PageImpl<>(List.of(card1, card2), pageRequest, 2);

        when(creditCardRepository.findAllByAccountId(eq(accountId), any())).thenReturn(cardPage);
        when(creditCardMapper.toDTO(card1)).thenReturn(dto1);
        when(creditCardMapper.toDTO(card2)).thenReturn(dto2);
        when(billingService.computeAvailableLimit(1L)).thenReturn(new BigDecimal("5000.00"));
        when(billingService.computeAvailableLimit(2L)).thenReturn(new BigDecimal("3000.00"));
        when(billingService.computeCurrentBill(anyLong(), anyInt(), anyInt()))
                .thenReturn(new CreditCardResponseDTO.CurrentBillDTO());

        var result = service.findAll(accountId, page, size);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(creditCardRepository).findAllByAccountId(eq(accountId), any());
        verify(creditCardMapper, times(2)).toDTO(any(CreditCard.class));
        verify(billingService).computeAvailableLimit(1L);
        verify(billingService).computeAvailableLimit(2L);
    }

    @Test
    void findAll_whenNoCreditCards_shouldReturnEmptyPage() {
        var accountId = 1L;
        var page = 0;
        var size = 20;

        var pageRequest = PageRequest.of(page, size);
        var emptyPage = new PageImpl<CreditCard>(List.of(), pageRequest, 0);

        when(creditCardRepository.findAllByAccountId(eq(accountId), any())).thenReturn(emptyPage);

        var result = service.findAll(accountId, page, size);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        verify(creditCardRepository).findAllByAccountId(eq(accountId), any());
        verifyNoInteractions(creditCardMapper, billingService);
    }

    @Test
    void findById_whenCreditCardExists_shouldReturnDTO() {
        var cardId = 1L;
        var accountId = 1L;
        var card = CreditCard.builder()
                .id(cardId)
                .nickname("Visa Gold")
                .closingDay(15)
                .dueDay(25)
                .build();

        var dto = new CreditCardResponseDTO();
        dto.setId(cardId);

        when(creditCardRepository.findByIdAndAccountId(cardId, accountId)).thenReturn(Optional.of(card));
        when(creditCardMapper.toDTO(card)).thenReturn(dto);
        when(billingService.computeAvailableLimit(cardId)).thenReturn(new BigDecimal("5000.00"));
        when(billingService.computeCurrentBill(cardId, 15, 25))
                .thenReturn(new CreditCardResponseDTO.CurrentBillDTO());

        var result = service.findById(cardId, accountId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(cardId);
        verify(creditCardRepository).findByIdAndAccountId(cardId, accountId);
        verify(creditCardMapper).toDTO(card);
        verify(billingService).computeAvailableLimit(cardId);
        verify(billingService).computeCurrentBill(cardId, 15, 25);
    }

    @Test
    void findById_whenCreditCardNotFound_shouldThrowException() {
        var cardId = 999L;
        var accountId = 1L;

        when(creditCardRepository.findByIdAndAccountId(cardId, accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(cardId, accountId))
                .isInstanceOf(com.relyon.credflow.exception.ResourceNotFoundException.class);

        verify(creditCardRepository).findByIdAndAccountId(cardId, accountId);
        verifyNoInteractions(creditCardMapper, billingService);
    }

    @Test
    void create_whenHolderExists_shouldCreateCreditCard() {
        var accountId = 1L;
        var holderId = 10L;
        var account = Account.builder().id(accountId).build();
        var holder = User.builder().id(holderId).name("John Doe").build();

        var card = CreditCard.builder()
                .nickname("Visa Gold")
                .brand("Visa")
                .tier("Gold")
                .issuer("Bank XYZ")
                .lastFourDigits("1234")
                .closingDay(15)
                .dueDay(25)
                .creditLimit(new BigDecimal("10000.00"))
                .build();

        var savedCard = CreditCard.builder()
                .id(1L)
                .nickname("Visa Gold")
                .account(account)
                .holder(holder)
                .build();

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(userRepository.findByIdAndAccountId(holderId, accountId)).thenReturn(Optional.of(holder));
        when(creditCardRepository.save(any(CreditCard.class))).thenReturn(savedCard);

        var result = service.create(card, accountId, holderId);

        assertThat(result).isNotNull();
        assertThat(result.getAccount()).isNotNull();
        assertThat(result.getAccount().getId()).isEqualTo(accountId);
        assertThat(result.getHolder()).isEqualTo(holder);

        verify(accountRepository).findById(accountId);
        verify(userRepository).findByIdAndAccountId(holderId, accountId);
        verify(creditCardRepository).save(card);
    }

    @Test
    void create_whenHolderNotFound_shouldThrowException() {
        var accountId = 1L;
        var holderId = 999L;
        var account = Account.builder().id(accountId).build();

        var card = CreditCard.builder()
                .nickname("Visa Gold")
                .build();

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(userRepository.findByIdAndAccountId(holderId, accountId)).thenReturn(Optional.empty());
        when(translationService.translateMessage("creditCard.holderNotFound"))
                .thenReturn("Holder not found");

        assertThatThrownBy(() -> service.create(card, accountId, holderId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Holder not found");

        verify(accountRepository).findById(accountId);
        verify(userRepository).findByIdAndAccountId(holderId, accountId);
        verify(translationService).translateMessage("creditCard.holderNotFound");
        verifyNoInteractions(creditCardRepository);
    }

    @Test
    void create_whenHolderFromDifferentAccount_shouldThrowException() {
        var accountId = 1L;
        var holderId = 10L;
        var account = Account.builder().id(accountId).build();

        var card = CreditCard.builder()
                .nickname("Visa Gold")
                .build();

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(userRepository.findByIdAndAccountId(holderId, accountId)).thenReturn(Optional.empty());
        when(translationService.translateMessage("creditCard.holderNotFound"))
                .thenReturn("Holder not found");

        assertThatThrownBy(() -> service.create(card, accountId, holderId))
                .isInstanceOf(IllegalArgumentException.class);

        verify(accountRepository).findById(accountId);
        verify(userRepository).findByIdAndAccountId(holderId, accountId);
    }

    @Test
    void findAllSelectByAccount_whenCreditCardsExist_shouldReturnSelectDTOs() {
        var accountId = 1L;
        var card1 = CreditCard.builder()
                .id(1L)
                .nickname("Visa Gold")
                .lastFourDigits("1234")
                .build();
        var card2 = CreditCard.builder()
                .id(2L)
                .nickname("Mastercard Platinum")
                .lastFourDigits("5678")
                .build();

        when(creditCardRepository.findAllByAccountId(accountId)).thenReturn(List.of(card1, card2));

        var result = service.findAllSelectByAccount(accountId);

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().getId()).isEqualTo(1L);
        assertThat(result.getFirst().getDescription()).isEqualTo("Visa Gold - 1234");
        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(1).getDescription()).isEqualTo("Mastercard Platinum - 5678");

        verify(creditCardRepository).findAllByAccountId(accountId);
    }

    @Test
    void findAllSelectByAccount_whenNoCreditCards_shouldReturnEmptyList() {
        var accountId = 1L;

        when(creditCardRepository.findAllByAccountId(accountId)).thenReturn(List.of());

        var result = service.findAllSelectByAccount(accountId);

        assertThat(result).isEmpty();
        verify(creditCardRepository).findAllByAccountId(accountId);
    }

    @Test
    void findAllSelectByAccount_shouldFormatDescriptionCorrectly() {
        var accountId = 1L;
        var card = CreditCard.builder()
                .id(1L)
                .nickname("My Card")
                .lastFourDigits("9999")
                .build();

        when(creditCardRepository.findAllByAccountId(accountId)).thenReturn(List.of(card));

        var result = service.findAllSelectByAccount(accountId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getDescription()).isEqualTo("My Card - 9999");
    }

    @Test
    void update_whenCardExists_shouldUpdateSuccessfully() {
        var cardId = 1L;
        var accountId = 1L;
        var holderId = 10L;

        var existing = CreditCard.builder()
                .id(cardId)
                .nickname("Old Nickname")
                .brand("Visa")
                .tier("Gold")
                .build();

        var updated = CreditCard.builder()
                .nickname("New Nickname")
                .brand("Mastercard")
                .tier("Platinum")
                .issuer("Bank ABC")
                .lastFourDigits("5678")
                .closingDay(20)
                .dueDay(28)
                .creditLimit(new BigDecimal("15000.00"))
                .build();

        var holder = User.builder().id(holderId).name("John Doe").build();

        when(creditCardRepository.findByIdAndAccountId(cardId, accountId)).thenReturn(Optional.of(existing));
        when(userRepository.findByIdAndAccountId(holderId, accountId)).thenReturn(Optional.of(holder));
        when(creditCardRepository.save(any(CreditCard.class))).thenReturn(existing);

        var result = service.update(cardId, accountId, updated, holderId);

        assertThat(result).isNotNull();
        assertThat(result.getNickname()).isEqualTo("New Nickname");
        verify(creditCardRepository).findByIdAndAccountId(cardId, accountId);
        verify(userRepository).findByIdAndAccountId(holderId, accountId);
        verify(creditCardRepository).save(existing);
    }

    @Test
    void update_whenCardNotFound_shouldThrowException() {
        var cardId = 999L;
        var accountId = 1L;
        var updated = CreditCard.builder().nickname("New").build();

        when(creditCardRepository.findByIdAndAccountId(cardId, accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(cardId, accountId, updated, 10L))
                .isInstanceOf(com.relyon.credflow.exception.ResourceNotFoundException.class);

        verify(creditCardRepository).findByIdAndAccountId(cardId, accountId);
        verifyNoInteractions(userRepository);
        verify(creditCardRepository, never()).save(any());
    }

    @Test
    void delete_whenCardExists_shouldDeleteSuccessfully() {
        var cardId = 1L;
        var accountId = 1L;

        when(creditCardRepository.existsByIdAndAccountId(cardId, accountId)).thenReturn(true);

        service.delete(cardId, accountId);

        verify(creditCardRepository).existsByIdAndAccountId(cardId, accountId);
        verify(creditCardRepository).deleteById(cardId);
    }

    @Test
    void delete_whenCardNotFound_shouldThrowException() {
        var cardId = 999L;
        var accountId = 1L;

        when(creditCardRepository.existsByIdAndAccountId(cardId, accountId)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(cardId, accountId))
                .isInstanceOf(com.relyon.credflow.exception.ResourceNotFoundException.class);

        verify(creditCardRepository).existsByIdAndAccountId(cardId, accountId);
        verify(creditCardRepository, never()).deleteById(any());
    }
}
