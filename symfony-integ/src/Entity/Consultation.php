<?php
namespace App\Entity;
use App\Repository\ConsultationRepository;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity(repositoryClass: ConsultationRepository::class)]
#[ORM\Table(name: 'consultation')]
class Consultation
{
    #[ORM\Id] #[ORM\GeneratedValue] #[ORM\Column]
    private ?int $id = null;

    #[ORM\Column(name: 'patient_id')]
    private ?int $patientId = null;

    #[ORM\Column(name: 'patologie_id', nullable: true)]
    private ?int $patologieId = null;

    #[ORM\Column(name: 'date_consultation', type: 'datetime')]
    private ?\DateTimeInterface $dateConsultation = null;

    #[ORM\Column(type: 'text', nullable: true)]
    private ?string $motif = null;

    #[ORM\Column(type: 'text', nullable: true)]
    private ?string $diagnostic = null;

    #[ORM\Column(type: 'text', nullable: true)]
    private ?string $observations = null;

    #[ORM\Column(type: 'text', nullable: true)]
    private ?string $ordonnance = null;

    #[ORM\Column(length: 50, options: ['default' => 'planifiée'])]
    private string $statut = 'planifiée';

    // Non-mapped
    private ?string $patientNom = null;
    private ?string $patientEmail = null;
    private ?string $patologieNom = null;

    public function getId(): ?int { return $this->id; }
    public function getPatientId(): ?int { return $this->patientId; }
    public function setPatientId(int $v): static { $this->patientId = $v; return $this; }
    public function getPatologieId(): ?int { return $this->patologieId; }
    public function setPatologieId(?int $v): static { $this->patologieId = $v; return $this; }
    public function getDateConsultation(): ?\DateTimeInterface { return $this->dateConsultation; }
    public function setDateConsultation(?\DateTimeInterface $v): static { $this->dateConsultation = $v; return $this; }
    public function getMotif(): ?string { return $this->motif; }
    public function setMotif(?string $v): static { $this->motif = $v; return $this; }
    public function getDiagnostic(): ?string { return $this->diagnostic; }
    public function setDiagnostic(?string $v): static { $this->diagnostic = $v; return $this; }
    public function getObservations(): ?string { return $this->observations; }
    public function setObservations(?string $v): static { $this->observations = $v; return $this; }
    public function getOrdonnance(): ?string { return $this->ordonnance; }
    public function setOrdonnance(?string $v): static { $this->ordonnance = $v; return $this; }
    public function getStatut(): string { return $this->statut; }
    public function setStatut(string $v): static { $this->statut = $v; return $this; }
    public function getPatientNom(): ?string { return $this->patientNom; }
    public function setPatientNom(?string $v): static { $this->patientNom = $v; return $this; }
    public function getPatientEmail(): ?string { return $this->patientEmail; }
    public function setPatientEmail(?string $v): static { $this->patientEmail = $v; return $this; }
    public function getPatologieNom(): ?string { return $this->patologieNom; }
    public function setPatologieNom(?string $v): static { $this->patologieNom = $v; return $this; }
}
