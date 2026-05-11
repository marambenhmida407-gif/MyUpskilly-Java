<?php
namespace App\Service;

use Doctrine\DBAL\Connection;

class StockService
{
    public function __construct(private Connection $conn) {}

    public function searchByName(string $query): array
    {
        return $this->conn->fetchAllAssociative(
            'SELECT * FROM stock_medicament WHERE LOWER(nom) LIKE ? ORDER BY nom',
            ['%' . strtolower($query) . '%']
        );
    }

    public function findAll(): array
    {
        return $this->conn->fetchAllAssociative(
            'SELECT * FROM stock_medicament ORDER BY nom'
        );
    }

    public function save(array $data): void
    {
        $this->conn->insert('stock_medicament', [
            'nom'          => $data['nom'],
            'forme'        => $data['forme'] ?? null,
            'dosage'       => $data['dosage'] ?? null,
            'quantite'     => (int) $data['quantite'],
            'unite'        => $data['unite'] ?? 'comprimé',
            'seuil_alerte' => (int) ($data['seuil_alerte'] ?? 10),
            'prix'         => (float) ($data['prix'] ?? 0),
            'fournisseur'  => $data['fournisseur'] ?? null,
            'date_expiry'  => $data['date_expiry'] ?? null,
        ]);
    }

    public function update(int $id, array $data): void
    {
        $this->conn->update('stock_medicament', [
            'nom'          => $data['nom'],
            'forme'        => $data['forme'] ?? null,
            'dosage'       => $data['dosage'] ?? null,
            'quantite'     => (int) $data['quantite'],
            'unite'        => $data['unite'] ?? 'comprimé',
            'seuil_alerte' => (int) ($data['seuil_alerte'] ?? 10),
            'prix'         => (float) ($data['prix'] ?? 0),
            'fournisseur'  => $data['fournisseur'] ?? null,
            'date_expiry'  => $data['date_expiry'] ?? null,
        ], ['id' => $id]);
    }

    public function delete(int $id): void
    {
        $this->conn->delete('stock_medicament', ['id' => $id]);
    }

    // ── RxNorm API ────────────────────────────────────────────────────────────

    public function verifyWithRxNorm(string $drugName): array
    {
        $encoded = urlencode($drugName);

        // Essai 1 : getDrugs (exact + brand names)
        $result = $this->queryRxDrugs($encoded);
        if ($result['found']) return $result;

        // Essai 2 : approximateMatch (typos, noms français)
        return $this->queryRxApproximate($encoded);
    }

    private function queryRxDrugs(string $encoded): array
    {
        $json = $this->httpGet(
            "https://rxnav.nlm.nih.gov/REST/drugs.json?name=$encoded"
        );
        if (!$json || !str_contains($json, '"rxcui"')) {
            return ['found' => false];
        }
        return [
            'found'   => true,
            'rxcui'   => $this->extractField($json, 'rxcui'),
            'name'    => $this->extractField($json, 'name'),
            'tty'     => $this->extractField($json, 'tty'),
            'synonym' => $this->extractField($json, 'synonym'),
        ];
    }


    private function queryRxApproximate(string $encoded): array
{
    $json = $this->httpGet(
        "https://rxnav.nlm.nih.gov/REST/approximateTerm.json?term=$encoded&maxEntries=3"
    );
    if (!$json || !str_contains($json, '"rxcui"')) {
        return ['found' => false];
    }

    // Extraire score — peut être un float dans certaines versions de l'API
    $scoreRaw = $this->extractField($json, 'score');
    $score    = (float) $scoreRaw;

    // Seuil abaissé à 50 pour couvrir les noms français/commerciaux
    if ($score > 0 && $score < 50) return ['found' => false];

    $rxcui = $this->extractField($json, 'rxcui');
    if (empty($rxcui)) return ['found' => false];

    return $this->queryRxProperties($rxcui);
}

    




    private function queryRxProperties(string $rxcui): array
    {
        $json = $this->httpGet(
            "https://rxnav.nlm.nih.gov/REST/rxcui/$rxcui/properties.json"
        );
        if (!$json) return ['found' => false];
        return [
            'found'   => true,
            'rxcui'   => $rxcui,
            'name'    => $this->extractField($json, 'name'),
            'tty'     => $this->extractField($json, 'tty'),
            'synonym' => '',
        ];
    }

    private function httpGet(string $url): ?string
    {
        $ctx = stream_context_create([
            'http' => [
                'timeout' => 7,
                'header'  => "Accept: application/json\r\n"
                           . "User-Agent: EspritMedical/1.0 (student project)\r\n",
            ],
        ]);
        $response = @file_get_contents($url, false, $ctx);
        return $response ?: null;
    }

    private function extractField(string $json, string $field): string
    {
        $key   = "\"$field\":\"";
        $start = strpos($json, $key);
        if ($start === false) return '';
        $start += strlen($key);
        $end = strpos($json, '"', $start);
        return $end !== false ? substr($json, $start, $end - $start) : '';
    }

    public function ttyLabel(string $tty): string
    {
        return match($tty) {
            'IN'  => 'Ingrédient générique',
            'BN'  => 'Nom commercial',
            'PIN' => 'Ingrédient précis',
            'SCD' => 'Médicament (générique)',
            'SBD' => 'Médicament (marque)',
            default => $tty ?: 'Médicament',
        };
    }
}