<?php
namespace App\Entity;
use App\Repository\SuiviTherapeutiqueRepository;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity(repositoryClass: SuiviTherapeutiqueRepository::class)]
#[ORM\Table(name: 'suivi_therapeutique')]
class SuiviTherapeutique
{
    #[ORM\Id] #[ORM\GeneratedValue] #[ORM\Column]
    private ?int $id = null;
    #[ORM\Column(name: 'patient_id')]                         private ?int $patientId = null;
    #[ORM\Column(name: 'date_debut', type: 'datetime')]       private ?\DateTimeInterface $dateDebut = null;
    #[ORM\Column(name: 'date_fin', type: 'datetime', nullable: true)] private ?\DateTimeInterface $dateFin = null;
    #[ORM\Column(name: 'type_suivi', length: 100)]            private ?string $typeSuivi = null;
    #[ORM\Column(name: 'objectif_therapeutique', length: 255)] private ?string $objectifTherapeutique = null;
    #[ORM\Column(length: 50)]                                  private ?string $statut = null;

    private ?string $patientNom = null;

    public function getId(): ?int { return $this->id; }
    public function getPatientId(): ?int { return $this->patientId; }
    public function setPatientId(int $v): static { $this->patientId = $v; return $this; }
    public function getDateDebut(): ?\DateTimeInterface { return $this->dateDebut; }
    public function setDateDebut(\DateTimeInterface $v): static { $this->dateDebut = $v; return $this; }
    public function getDateFin(): ?\DateTimeInterface { return $this->dateFin; }
    public function setDateFin(?\DateTimeInterface $v): static { $this->dateFin = $v; return $this; }
    public function getTypeSuivi(): ?string { return $this->typeSuivi; }
    public function setTypeSuivi(string $v): static { $this->typeSuivi = $v; return $this; }
    public function getObjectifTherapeutique(): ?string { return $this->objectifTherapeutique; }
    public function setObjectifTherapeutique(string $v): static { $this->objectifTherapeutique = $v; return $this; }
    public function getStatut(): ?string { return $this->statut; }
    public function setStatut(string $v): static { $this->statut = $v; return $this; }
    public function getPatientNom(): ?string { return $this->patientNom; }
    public function setPatientNom(?string $v): static { $this->patientNom = $v; return $this; }
}
