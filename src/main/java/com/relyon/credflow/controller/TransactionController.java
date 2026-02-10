package com.relyon.credflow.controller;

import com.relyon.credflow.model.mapper.TransactionMapper;
import com.relyon.credflow.model.transaction.*;
import com.relyon.credflow.model.user.AuthenticatedUser;
import com.relyon.credflow.service.DuplicateDetectionService;
import com.relyon.credflow.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/transactions")
@Slf4j
@Tag(name = "Transações", description = "Endpoints para gerenciamento de transações financeiras")
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;
    private final DuplicateDetectionService duplicateDetectionService;

    @Operation(
            summary = "Importar fatura do Banrisul",
            description = "Importa transações a partir de um arquivo CSV no formato do Banrisul. " +
                    "O sistema detecta automaticamente duplicatas via checksum e cria mapeamentos de descrição " +
                    "para futuras importações. Também executa detecção automática de estornos/reembolsos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transações importadas com sucesso"),
            @ApiResponse(responseCode = "400", description = "Arquivo inválido ou formato incorreto", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content)
    })
    @PostMapping("/import/csv/banrisul")
    public ResponseEntity<List<TransactionResponseDTO>> importBanrisulCSV(
            @Parameter(description = "Arquivo CSV do extrato do Banrisul", required = true)
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("POST /import/csv/banrisul for account {}", user.getAccountId());
        var transactions = transactionService.importFromBanrisulCSV(file, user.getAccountId());
        var response = transactions.stream().map(transactionMapper::toDto).toList();
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Criar nova transação",
            description = "Cria uma nova transação financeira manualmente. O sistema automaticamente: " +
                    "1) Define a origem como MANUAL, " +
                    "2) Executa detecção de estornos/reembolsos (busca ±90 dias por valores opostos), " +
                    "3) Cria mapeamento de descrição para futuras categorizações automáticas, " +
                    "4) Verifica potenciais duplicatas com transações importadas (±3 dias, mesmo valor)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transação criada com sucesso. Se houver potenciais duplicatas, elas são retornadas no campo 'potentialDuplicates'"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content)
    })
    @PostMapping
    public ResponseEntity<TransactionCreateResponseDTO> create(
            @Parameter(description = "Dados da transação a ser criada", required = true)
            @Valid @RequestBody TransactionRequestDTO dto,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("POST /v1/transactions to create transaction for account {}", user.getAccountId());
        var transaction = transactionMapper.toEntity(dto);
        var created = transactionService.create(transaction, user.getAccountId());

        var potentialDuplicates = duplicateDetectionService.findPotentialDuplicatesForManualEntry(
                user.getAccountId(),
                created.getDate(),
                created.getValue()
        );

        if (!potentialDuplicates.isEmpty()) {
            log.info("Found {} potential duplicates for transaction {}", potentialDuplicates.size(), created.getId());
        }

        var response = TransactionCreateResponseDTO.builder()
                .transaction(transactionMapper.toDto(created))
                .potentialDuplicates(potentialDuplicates)
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Buscar transações com filtros",
            description = "Busca transações aplicando filtros opcionais: período, valor, descrição, categoria, " +
                    "responsáveis, cartão de crédito. Por padrão, exclui transações marcadas como estorno/reembolso " +
                    "(use includeReversals=true para incluí-las). Suporta ordenação customizada."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Busca realizada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content)
    })
    @PostMapping("/search")
    public ResponseEntity<List<TransactionResponseDTO>> findFiltered(
            @Parameter(description = "Filtros de busca (todos opcionais)")
            @RequestBody(required = false) TransactionFilter transactionFilter,
            @Parameter(description = "Ordenação (ex: date,desc ou value,asc)")
            @ParameterObject Sort sort,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        var result = transactionService.search(transactionFilter, sort).stream().map(transactionMapper::toDto).toList();
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Buscar transação por ID",
            description = "Retorna os detalhes completos de uma transação específica pelo ID. " +
                    "Inclui informações de origem, categoria, responsáveis, cartão e flags de estorno."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transação encontrada"),
            @ApiResponse(responseCode = "404", description = "Transação não encontrada", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponseDTO> findById(
            @Parameter(description = "ID da transação", required = true)
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("GET /v1/transactions/{} for account {}", id, user.getAccountId());
        return transactionService.findById(id, user.getAccountId())
                .map(transactionMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Atualizar transação",
            description = "Atualiza uma transação existente. Se a transação foi importada (origem diferente de MANUAL), " +
                    "ela será marcada como 'editada após importação'. Executa nova detecção de estornos após a atualização."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transação atualizada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Transação não encontrada", content = @Content),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponseDTO> update(
            @Parameter(description = "ID da transação a ser atualizada", required = true)
            @PathVariable Long id,
            @Parameter(description = "Novos dados da transação", required = true)
            @Valid @RequestBody TransactionRequestDTO dto,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("PUT /v1/transactions/{} for account {}", id, user.getAccountId());
        Transaction patch = transactionMapper.toEntity(dto);
        var updated = transactionService.update(id, patch, user.getAccountId());
        return ResponseEntity.ok(transactionMapper.toDto(updated));
    }

    @Operation(
            summary = "Excluir transação",
            description = "Exclui permanentemente uma transação específica pelo ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Transação excluída com sucesso"),
            @ApiResponse(responseCode = "404", description = "Transação não encontrada", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID da transação a ser excluída", required = true)
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("DELETE /v1/transactions/{} for account {}", id, user.getAccountId());
        transactionService.delete(id, user.getAccountId());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Excluir múltiplas transações",
            description = "Exclui permanentemente várias transações de uma vez fornecendo uma lista de IDs. " +
                    "Útil para limpeza em massa ou remoção de importações incorretas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Transações excluídas com sucesso"),
            @ApiResponse(responseCode = "404", description = "Uma ou mais transações não encontradas", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content)
    })
    @DeleteMapping
    public ResponseEntity<Void> deleteMultiple(
            @Parameter(description = "Lista de IDs das transações a serem excluídas", required = true, example = "1,2,3")
            @RequestParam List<Long> ids,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("DELETE /v1/transactions for {} transactions in account {}", ids.size(), user.getAccountId());
        transactionService.bulkDelete(ids, user.getAccountId());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Atualizar categoria de múltiplas transações",
            description = "Atualiza a categoria de várias transações simultaneamente. " +
                    "Transações importadas são marcadas como 'editadas após importação'. " +
                    "Para remover a categoria, envie categoryId como null."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categorias atualizadas com sucesso"),
            @ApiResponse(responseCode = "404", description = "Uma ou mais transações não encontradas", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content)
    })
    @PutMapping("/category")
    public ResponseEntity<List<TransactionResponseDTO>> updateCategory(
            @Parameter(description = "Lista de IDs das transações", required = true, example = "1,2,3")
            @RequestParam List<Long> ids,
            @Parameter(description = "ID da nova categoria (ou null para remover)")
            @RequestParam(required = false) Long categoryId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("PUT /v1/transactions/category for {} transactions in account {}", ids.size(), user.getAccountId());
        var updated = transactionService.bulkUpdateCategory(ids, categoryId, user.getAccountId());
        var response = updated.stream().map(transactionMapper::toDto).toList();
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Atualizar responsáveis de múltiplas transações",
            description = "Define os responsáveis para várias transações simultaneamente. " +
                    "Substitui completamente a lista de responsáveis existente. " +
                    "Transações importadas são marcadas como 'editadas após importação'."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Responsáveis atualizados com sucesso"),
            @ApiResponse(responseCode = "404", description = "Uma ou mais transações/usuários não encontrados", content = @Content),
            @ApiResponse(responseCode = "400", description = "Lista de responsáveis vazia ou usuários inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content)
    })
    @PutMapping("/responsible-users")
    public ResponseEntity<List<TransactionResponseDTO>> updateResponsibleUsers(
            @Parameter(description = "Lista de IDs das transações", required = true, example = "1,2,3")
            @RequestParam List<Long> ids,
            @Parameter(description = "Lista de IDs dos usuários responsáveis", required = true, example = "5,7")
            @RequestParam List<Long> responsibleUserIds,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("PUT /v1/transactions/responsible-users for {} transactions in account {}", ids.size(), user.getAccountId());
        var updated = transactionService.bulkUpdateResponsibleUsers(ids, responsibleUserIds, user.getAccountId());
        var response = updated.stream().map(transactionMapper::toDto).toList();
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Corrigir sinais de transações CSV de cartão de crédito",
            description = "Nega o valor de transações importadas via CSV que estão armazenadas como positivas (deviam ser negativas). " +
                    "Exclui linhas que contêm 'PGTO' (pagamentos já são negativos). " +
                    "Operação idempotente: re-executar encontra 0 transações."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Correção executada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content)
    })
    @PostMapping("/fix-csv-signs")
    public ResponseEntity<Map<String, Object>> fixCsvSigns(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("POST /v1/transactions/fix-csv-signs for account {}", user.getAccountId());
        var count = transactionService.negatePositiveCsvImportedTransactions(user.getAccountId());
        return ResponseEntity.ok(Map.of("correctedTransactions", count));
    }

    @Operation(
            summary = "Listar potenciais duplicatas",
            description = "Retorna grupos de transações que podem ser duplicatas. " +
                    "Uma duplicata potencial é identificada quando existem transações com o mesmo valor " +
                    "dentro de uma janela de ±3 dias, sendo uma de origem CSV_IMPORT e outra MANUAL. " +
                    "Duplicatas entre transações do mesmo tipo (CSV-CSV ou MANUAL-MANUAL) não são reportadas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de grupos de potenciais duplicatas"),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content)
    })
    @GetMapping("/duplicates")
    public ResponseEntity<List<DuplicateGroupDTO>> findPotentialDuplicates(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("GET /v1/transactions/duplicates for account {}", user.getAccountId());
        var duplicates = duplicateDetectionService.findAllPotentialDuplicates(user.getAccountId());
        log.info("Found {} potential duplicate groups for account {}", duplicates.size(), user.getAccountId());
        return ResponseEntity.ok(duplicates);
    }
}
