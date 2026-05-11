<?php

namespace App\Entity;

use App\Repository\QuestionRepository;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity(repositoryClass: QuestionRepository::class)]
#[ORM\Table(name: 'questions')]
class Question
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column]
    private ?int $id = null;

    #[ORM\Column(name: 'user_id')]
    private ?int $userId = null;

    #[ORM\Column(length: 255, nullable: true)]
    private ?string $categorie = null;

    #[ORM\Column(length: 255)]
    private ?string $titre = null;

    #[ORM\Column(type: 'text')]
    private ?string $description = null;

    #[ORM\Column(name: 'sous_traitement', nullable: true)]
    private ?bool $sousTraitement = null;

    #[ORM\Column(name: 'a_des_allergies', nullable: true)]
    private ?bool $aDesAllergies = null;

    #[ORM\Column(name: 'fichier_chemin', length: 255, nullable: true)]
    private ?string $fichierChemin = null;

    #[ORM\Column(length: 50, options: ['default' => 'en_attente'])]
    private string $statut = 'en_attente';

    #[ORM\Column(nullable: true)]
    private ?float $taille = null;

    #[ORM\Column(nullable: true)]
    private ?float $poids = null;

    #[ORM\Column(name: 'description_traitement', type: 'text', nullable: true)]
    private ?string $descriptionTraitement = null;

    #[ORM\Column(name: 'description_allergies', type: 'text', nullable: true)]
    private ?string $descriptionAllergies = null;

    #[ORM\Column(name: 'created_at', type: 'datetime', nullable: true)]
    private ?\DateTimeInterface $createdAt = null;

    // Non-mapped field for user display
    private ?string $userNom = null;
    private ?string $userPrenom = null;

    public function getId(): ?int { return $this->id; }
    public function getUserId(): ?int { return $this->userId; }
    public function setUserId(int $userId): static { $this->userId = $userId; return $this; }
    public function getCategorie(): ?string { return $this->categorie; }
    public function setCategorie(?string $categorie): static { $this->categorie = $categorie; return $this; }
    public function getTitre(): ?string { return $this->titre; }
    public function setTitre(string $titre): static { $this->titre = $titre; return $this; }
    public function getDescription(): ?string { return $this->description; }
    public function setDescription(string $description): static { $this->description = $description; return $this; }
    public function isSousTraitement(): ?bool { return $this->sousTraitement; }
    public function setSousTraitement(?bool $v): static { $this->sousTraitement = $v; return $this; }
    public function isADesAllergies(): ?bool { return $this->aDesAllergies; }
    public function setADesAllergies(?bool $v): static { $this->aDesAllergies = $v; return $this; }
    public function getFichierChemin(): ?string { return $this->fichierChemin; }
    public function setFichierChemin(?string $v): static { $this->fichierChemin = $v; return $this; }
    public function getStatut(): string { return $this->statut; }
    public function setStatut(string $statut): static { $this->statut = $statut; return $this; }
    public function getTaille(): ?float { return $this->taille; }
    public function setTaille(?float $taille): static { $this->taille = $taille; return $this; }
    public function getPoids(): ?float { return $this->poids; }
    public function setPoids(?float $poids): static { $this->poids = $poids; return $this; }
    public function getDescriptionTraitement(): ?string { return $this->descriptionTraitement; }
    public function setDescriptionTraitement(?string $v): static { $this->descriptionTraitement = $v; return $this; }
    public function getDescriptionAllergies(): ?string { return $this->descriptionAllergies; }
    public function setDescriptionAllergies(?string $v): static { $this->descriptionAllergies = $v; return $this; }
    public function getCreatedAt(): ?\DateTimeInterface { return $this->createdAt; }
    public function setCreatedAt(?\DateTimeInterface $v): static { $this->createdAt = $v; return $this; }
    public function getUserNom(): ?string { return $this->userNom; }
    public function setUserNom(?string $v): static { $this->userNom = $v; return $this; }
    public function getUserPrenom(): ?string { return $this->userPrenom; }
    public function setUserPrenom(?string $v): static { $this->userPrenom = $v; return $this; }
}
