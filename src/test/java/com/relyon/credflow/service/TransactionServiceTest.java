package com.relyon.credflow.service;

import com.relyon.credflow.exception.CsvProcessingException;
import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.model.descriptionmapping.DescriptionMapping;
import com.relyon.credflow.model.transaction.Transaction;
import com.relyon.credflow.repository.DescriptionMappingRepository;
import com.relyon.credflow.repository.TransactionRepository;
import com.relyon.credflow.utils.NormalizationUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository repository;

    @Mock
    private DescriptionMappingRepository mappingRepository;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private TransactionService service;

    @Test
    void importFromBanrisulCSV_withExistingMapping_savesTransaction_usesMapping_andNoNewMappingsSaved() throws Exception {
        var accountId = 10L;
        var account = new Account();
        account.setId(accountId);

        var desc = "Loja X 12/03 #123";
        var norm = NormalizationUtils.normalizeDescription(desc);

        var mapping = new DescriptionMapping();
        mapping.setNormalizedDescription(norm);
        mapping.setOriginalDescription(desc);
        mapping.setSimplifiedDescription("Loja X");
        var cat = new Category();
        mapping.setCategory(cat);

        when(accountService.findById(accountId)).thenReturn(account);
        when(mappingRepository.findAllByAccountId(accountId)).thenReturn(List.of(mapping));

        var csv = ("Header;Ignore\n" +
                "12/03/2025;\""+desc+"\";R$ 1.234,56;X\n").getBytes(StandardCharsets.UTF_8);
        var file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("file.csv");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csv));

        when(repository.existsByChecksum(anyString())).thenReturn(false);
        var saved = new Transaction();
        when(repository.save(any(Transaction.class))).thenReturn(saved);

        var result = service.importFromBanrisulCSV(file, accountId);

        assertEquals(1, result.size());
        verify(repository, times(1)).save(any(Transaction.class));
        verify(mappingRepository, never()).saveAll(anyCollection());
    }

    @Test
    void importFromBanrisulCSV_withoutMapping_createsNewMapping_andSavesIt() throws Exception {
        var accountId = 11L;
        var account = new Account();
        account.setId(accountId);

        when(accountService.findById(accountId)).thenReturn(account);
        when(mappingRepository.findAllByAccountId(accountId)).thenReturn(List.of());

        var desc = "Super Y 04/05";
        var csv = ("x;y\n" +
                "04/05/2025;\"" + desc + "\";R$ 12,34;X\n").getBytes(StandardCharsets.UTF_8);
        var file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("file.csv");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csv));

        when(repository.existsByChecksum(anyString())).thenReturn(false);
        when(repository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.importFromBanrisulCSV(file, accountId);

        assertEquals(1, result.size());
        verify(mappingRepository, times(1))
                .saveAll(argThat(iterable -> {
                    int count = 0;
                    for (var ignored : iterable) count++;
                    return count == 1;
                }));
    }

    @Test
    void importFromBanrisulCSV_skipsDuplicateTransaction_byChecksum() throws Exception {
        var accountId = 12L;
        var account = new Account();
        account.setId(accountId);

        when(accountService.findById(accountId)).thenReturn(account);
        when(mappingRepository.findAllByAccountId(accountId)).thenReturn(List.of());

        var csv = ("h;i\n" +
                "01/01/2025;\"Any\";R$ 10,00;X\n").getBytes(StandardCharsets.UTF_8);
        var file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("file.csv");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csv));

        when(repository.existsByChecksum(anyString())).thenReturn(true);

        var res = service.importFromBanrisulCSV(file, accountId);

        assertEquals(0, res.size());
        verify(repository, never()).save(any());
        verify(mappingRepository, times(1)).saveAll(anyCollection());
    }

    @Test
    void importFromBanrisulCSV_onIoError_wrapsAndThrowsCsvProcessingException() throws Exception {
        var accountId = 13L;
        when(accountService.findById(accountId)).thenReturn(new Account());
        var file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("bad.csv");
        when(file.getInputStream()).thenThrow(new RuntimeException("IO"));

        assertThrows(CsvProcessingException.class, () -> service.importFromBanrisulCSV(file, accountId));
    }

    @Test
    void importFromBanrisulCSV_parsingError_returnsEmpty_doesNotSaveAnything() throws Exception {
        var accountId = 21L;
        var account = new Account(); account.setId(accountId);
        when(accountService.findById(accountId)).thenReturn(account);
        when(mappingRepository.findAllByAccountId(accountId)).thenReturn(List.of());

        var csv = ("header;h\n" +
                "12/03/2025;\"Bad Value\";R$ ABC;X\n").getBytes(StandardCharsets.UTF_8);
        var file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("bad.csv");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csv));

        var res = service.importFromBanrisulCSV(file, accountId);

        assertEquals(0, res.size());
        verify(repository, never()).save(any());
        verify(mappingRepository, never()).saveAll(any());
    }

    @Test
    void importFromBanrisulCSV_twoLinesSameNormalized_createsSingleNewMapping_savesTwoTransactions() throws Exception {
        var accountId = 22L;
        var account = new Account(); account.setId(accountId);
        when(accountService.findById(accountId)).thenReturn(account);
        when(mappingRepository.findAllByAccountId(accountId)).thenReturn(List.of());

        var desc = "Loja XPTO 10/10";
        var csv = ("h;i\n" +
                "10/10/2025;\""+desc+"\";R$ 10,00;X\n" +
                "10/10/2025;\""+desc+"\";R$ 20,00;X\n").getBytes(StandardCharsets.UTF_8);
        var file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("dup.csv");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csv));

        when(repository.existsByChecksum(anyString())).thenReturn(false);
        when(repository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        var res = service.importFromBanrisulCSV(file, accountId);

        assertEquals(2, res.size());
        verify(repository, times(2)).save(any(Transaction.class));
        verify(mappingRepository, times(1)).saveAll(argThat(iterable -> {
            int count = 0; for (var __ : iterable) count++; return count == 1;
        }));
    }

    @Test
    void importFromBanrisulCSV_setsChecksumOnSavedTransaction() throws Exception {
        var accountId = 23L;
        var account = new Account(); account.setId(accountId);
        when(accountService.findById(accountId)).thenReturn(account);
        when(mappingRepository.findAllByAccountId(accountId)).thenReturn(List.of());

        var line = "01/01/2025;\"ABC\";R$ 1,00;X";
        var csv = ("skip;skip\n" + line + "\n").getBytes(StandardCharsets.UTF_8);
        var file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("c.csv");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csv));

        when(repository.existsByChecksum(anyString())).thenReturn(false);
        var captor = ArgumentCaptor.forClass(Transaction.class);
        when(repository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        service.importFromBanrisulCSV(file, accountId);

        verify(repository).save(captor.capture());
        var saved = captor.getValue();
        assertNotNull(saved.getChecksum());
        assertEquals(org.apache.commons.codec.digest.DigestUtils.sha256Hex(line.trim()), saved.getChecksum());
    }

    @Test
    void importFromBanrisulCSV_existingMappingWithoutCategory_usesDefaultCategoryAndSetsAccount() throws Exception {
        var accountId = 55L;
        var account = new Account(); account.setId(accountId);

        var desc = "Sem Categoria 10/10";
        var normalized = NormalizationUtils.normalizeDescription(desc);

        var mapping = new DescriptionMapping();
        mapping.setOriginalDescription(desc);
        mapping.setNormalizedDescription(normalized);
        mapping.setSimplifiedDescription("Sem Categoria");
        mapping.setCategory(null); // triggers default "Não Identificado"

        when(accountService.findById(accountId)).thenReturn(account);
        when(mappingRepository.findAllByAccountId(accountId)).thenReturn(List.of(mapping));

        var csv = ("head;head\n" +
                "10/10/2025;\""+desc+"\";R$ 10,00;X\n").getBytes(StandardCharsets.UTF_8);
        var file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("m.csv");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csv));

        when(repository.existsByChecksum(anyString())).thenReturn(false);

        var captor = ArgumentCaptor.forClass(Transaction.class);
        when(repository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        var res = service.importFromBanrisulCSV(file, accountId);

        assertEquals(1, res.size());
        verify(repository).save(captor.capture());
        var saved = captor.getValue();
        assertEquals("Sem Categoria", saved.getSimplifiedDescription());
        assertNotNull(saved.getCategory());
        assertEquals("Não Identificado", saved.getCategory().getName());
        assertSame(account, saved.getCategory().getAccount());
        assertSame(account, saved.getAccount());
    }

    @Test
    void create_savesMappingIfMissing_thenSavesTransaction() {
        var acc = new Account();
        acc.setId(1L);

        var t = new Transaction();
        t.setDescription("Padaria 10/10");
        t.setSimplifiedDescription("Padaria");
        t.setCategory(new Category());
        t.setAccount(acc);

        when(mappingRepository.findByNormalizedDescriptionAndAccountId(anyString(), eq(1L)))
                .thenReturn(Optional.empty());
        when(repository.save(same(t))).thenReturn(t);

        var res = service.create(t);

        assertSame(t, res);
        verify(mappingRepository, times(1)).save(any(DescriptionMapping.class));
        verify(repository, times(1)).save(same(t));
    }

    @Test
    void create_whenMappingExists_doesNotSaveMapping() {
        var acc = new Account();
        acc.setId(2L);

        var t = new Transaction();
        t.setDescription("Mercado");
        t.setSimplifiedDescription("Mercado");
        t.setCategory(new Category());
        t.setAccount(acc);

        when(mappingRepository.findByNormalizedDescriptionAndAccountId(anyString(), eq(2L)))
                .thenReturn(Optional.of(new DescriptionMapping()));
        when(repository.save(same(t))).thenReturn(t);

        service.create(t);

        verify(mappingRepository, never()).save(any());
        verify(repository, times(1)).save(same(t));
    }

    @Test
    void update_whenFound_updatesFields_saves_andCreatesMappingIfMissing() {
        var id = 5L;
        var accountId = 3L;

        var account = new Account();
        account.setId(accountId);

        var existing = new Transaction();
        existing.setId(id);

        var updated = new Transaction();
        updated.setDate(LocalDate.parse("2025-01-01"));
        updated.setDescription("Restaurante 01/01");
        updated.setSimplifiedDescription("Restaurante");
        updated.setCategory(new Category());
        updated.setValue(updated.getValue());
        updated.setResponsible("Ela");

        when(repository.findByIdAndAccountId(id, accountId)).thenReturn(Optional.of(existing));
        when(accountService.findById(accountId)).thenReturn(account);
        when(mappingRepository.findByNormalizedDescriptionAndAccountId(anyString(), eq(accountId)))
                .thenReturn(Optional.empty());
        when(repository.save(same(existing))).thenReturn(existing);

        var res = service.update(id, updated, accountId);

        assertSame(existing, res);
        verify(mappingRepository, times(1)).save(any(DescriptionMapping.class));
        verify(repository, times(1)).save(same(existing));
    }

    @Test
    void update_whenNotFound_throwsResourceNotFound() {
        when(repository.findByIdAndAccountId(1L, 1L)).thenReturn(Optional.empty());
        var upd = new Transaction();
        assertThrows(ResourceNotFoundException.class, () -> service.update(1L, upd, 1L));
        verify(repository, never()).save(any());
    }

    @Test
    void findFiltered_withoutCategory_callsRepositoryVariantWithoutCategory_andUsesSortKey() {
        when(repository.findByAccountIdAndResponsibleAndDateBetween(
                eq(7L), eq("Ambos"), isNull(), isNull(), any(Sort.class)))
                .thenReturn(List.of(new Transaction()));

        var res = service.findFiltered(7L, null, null, null, null, "valueDesc");

        assertEquals(1, res.size());
        verify(repository, times(1)).findByAccountIdAndResponsibleAndDateBetween(
                eq(7L), eq("Ambos"), isNull(), isNull(), eq(Sort.by("value").descending()));
        verify(repository, never()).findByAccountIdAndResponsibleAndCategoryAndDateBetween(anyLong(), anyString(), anyString(), any(), any(), any());
    }

    @Test
    void findFiltered_withCategory_callsRepositoryVariantWithCategory_andDefaultSortDescDate() {
        when(repository.findByAccountIdAndResponsibleAndCategoryAndDateBetween(
                eq(8L), eq("Ela"), eq("Food"), isNull(), isNull(), any(Sort.class)))
                .thenReturn(List.of(new Transaction()));

        var res = service.findFiltered(8L, "Ela", "Food", null, null, "unknown");

        assertEquals(1, res.size());
        verify(repository, times(1)).findByAccountIdAndResponsibleAndCategoryAndDateBetween(
                eq(8L), eq("Ela"), eq("Food"), isNull(), isNull(), eq(Sort.by("date").descending()));
    }

    @Test
    void findFiltered_sortByDateAsc_withoutCategory() {
        when(repository.findByAccountIdAndResponsibleAndDateBetween(
                eq(5L), eq("Ambos"), isNull(), isNull(), any(Sort.class)))
                .thenReturn(List.of(new Transaction()));

        var res = service.findFiltered(5L, null, null, null, null, "date");

        assertEquals(1, res.size());
        verify(repository, times(1)).findByAccountIdAndResponsibleAndDateBetween(
                eq(5L), eq("Ambos"), isNull(), isNull(), eq(Sort.by("date").ascending()));
    }

    @Test
    void findFiltered_withCategory_andDescriptionDescSort() {
        when(repository.findByAccountIdAndResponsibleAndCategoryAndDateBetween(
                eq(6L), eq("Ela"), eq("Food"), isNull(), isNull(), any(Sort.class)))
                .thenReturn(List.of(new Transaction()));

        var res = service.findFiltered(6L, "Ela", "Food", null, null, "descriptionDesc");

        assertEquals(1, res.size());
        verify(repository, times(1)).findByAccountIdAndResponsibleAndCategoryAndDateBetween(
                eq(6L), eq("Ela"), eq("Food"), isNull(), isNull(), eq(Sort.by("description").descending()));
    }

    @Test
    void findFiltered_withoutCategory_parsesDates_andSortsByValueAsc() {
        when(repository.findByAccountIdAndResponsibleAndDateBetween(
                eq(42L),
                eq("Ele"),
                eq(LocalDate.parse("2025-01-01")),
                eq(LocalDate.parse("2025-01-31")),
                any(Sort.class))
        ).thenReturn(List.of(new Transaction()));

        var res = service.findFiltered(42L, "Ele", null, "2025-01-01", "2025-01-31", "value");

        assertEquals(1, res.size());
        verify(repository, times(1)).findByAccountIdAndResponsibleAndDateBetween(
                eq(42L),
                eq("Ele"),
                eq(LocalDate.parse("2025-01-01")),
                eq(LocalDate.parse("2025-01-31")),
                eq(Sort.by("value").ascending())
        );
    }

    @Test
    void findFiltered_withCategory_sortsByValueDesc() {
        when(repository.findByAccountIdAndResponsibleAndCategoryAndDateBetween(
                eq(43L), eq("Ambos"), eq("Bills"), isNull(), isNull(), any(Sort.class))
        ).thenReturn(List.of(new Transaction()));

        var res = service.findFiltered(43L, null, "Bills", null, null, "valueDesc");

        assertEquals(1, res.size());
        verify(repository, times(1)).findByAccountIdAndResponsibleAndCategoryAndDateBetween(
                eq(43L), eq("Ambos"), eq("Bills"), isNull(), isNull(), eq(Sort.by("value").descending())
        );
    }

    @Test
    void findFiltered_withCategory_sortsByDescriptionAsc() {
        when(repository.findByAccountIdAndResponsibleAndCategoryAndDateBetween(
                eq(44L), eq("Ela"), eq("Food"), isNull(), isNull(), any(Sort.class))
        ).thenReturn(List.of(new Transaction()));

        var res = service.findFiltered(44L, "Ela", "Food", null, null, "description");

        assertEquals(1, res.size());
        verify(repository, times(1)).findByAccountIdAndResponsibleAndCategoryAndDateBetween(
                eq(44L), eq("Ela"), eq("Food"), isNull(), isNull(), eq(Sort.by("description").ascending())
        );
    }

    @Test
    void findById_delegatesToRepository() {
        when(repository.findByIdAndAccountId(1L, 2L)).thenReturn(Optional.of(new Transaction()));
        var res = service.findById(1L, 2L);
        assertTrue(res.isPresent());
        verify(repository, times(1)).findByIdAndAccountId(1L, 2L);
    }

    @Test
    void delete_whenFound_deletesEntity() {
        var t = new Transaction();
        when(repository.findByIdAndAccountId(9L, 3L)).thenReturn(Optional.of(t));
        service.delete(9L, 3L);
        verify(repository, times(1)).delete(same(t));
    }

    @Test
    void delete_whenMissing_throws() {
        when(repository.findByIdAndAccountId(9L, 4L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.delete(9L, 4L));
        verify(repository, never()).delete(any());
    }

    @Test
    void applyMappingToExistingTransactions_updatesAndSavesAll() {
        var accountId = 15L;
        var original = "Old Desc";
        var simplified = "New Simp";
        var cat = new Category();

        var t1 = new Transaction();
        t1.setDescription(original);
        var t2 = new Transaction();
        t2.setDescription(original);

        when(repository.findByAccountIdAndDescriptionIgnoreCase(accountId, original))
                .thenReturn(List.of(t1, t2));

        service.applyMappingToExistingTransactions(accountId, original, simplified, cat);

        assertEquals(simplified, t1.getSimplifiedDescription());
        assertEquals(cat, t1.getCategory());
        assertEquals(simplified, t2.getSimplifiedDescription());
        assertEquals(cat, t2.getCategory());

        verify(repository, times(1)).saveAll(anyCollection());
    }
}